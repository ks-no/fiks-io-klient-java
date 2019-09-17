package no.ks.fiks.io.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.io.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.io.client.model.AmqpChannelFeedbackHandler;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.commons.FiksIOHeaders;
import no.ks.fiks.io.commons.FiksIOMeldingParser;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import no.ks.fiks.maskinporten.Maskinportenklient;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
class AmqpHandler implements Closeable {

    private static final String DOKUMENTLAGER_PAYLOAD_TYPE_HEADER = "dokumentlager-id";
    private final Channel channel;
    private final Predicate<MeldingId> meldingErBehandlet;
    private final DokumentlagerKlient dokumentlagerKlient;
    private KontoId kontoId;
    private final FiksIOHandler fiksIOHandler;
    private final AsicHandler asic;

    AmqpHandler(@NonNull AmqpKonfigurasjon amqpKonf,
                @NonNull FiksIntegrasjonKonfigurasjon intKonf,
                @NonNull FiksIOHandler fiksIOHandler,
                @NonNull AsicHandler asic,
                @NonNull Maskinportenklient maskinportenklient,
                @NonNull KontoId kontoId,
                @NonNull DokumentlagerKlient dokumentlagerKlient) {
        this.fiksIOHandler = fiksIOHandler;
        this.asic = asic;
        this.kontoId = kontoId;
        this.meldingErBehandlet = amqpKonf.getMeldingErBehandlet();
        this.dokumentlagerKlient = dokumentlagerKlient;

        ConnectionFactory factory = getConnectionFactory(amqpKonf, intKonf, maskinportenklient);

        try {
            channel = factory.newConnection().createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void newConsume(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        try {
            channel.basicConsume(FiksIOHeaders.getKontoQueueName(kontoId.getUuid()), (ct, m) -> {
                MottattMeldingMetadata parsed = FiksIOMeldingParser.parse(m.getEnvelope(), m.getProperties());

                if (m.getEnvelope().isRedeliver() && meldingErBehandlet.test(new MeldingId(parsed.getMeldingId()))) {
                    channel.basicAck(m.getEnvelope().getDeliveryTag(), false);
                } else {
                    MottattMelding melding = MottattMelding.fromMottattMeldingMetadata(
                        parsed,
                        f -> asic.writeDecrypted(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()), f),
                        f -> writeFile(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()), f),
                        () -> payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()),
                        () -> asic.decrypt(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody())));
                    onMelding.accept(melding, fiksIOHandler.buildKvitteringSender(amqpChannelFeedbackHandler(m.getEnvelope().getDeliveryTag())
                        , melding));
                }
            }, (consumerTag, sig) -> onClose.accept(sig));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AmqpChannelFeedbackHandler amqpChannelFeedbackHandler(final long deliveryTag) {
        return AmqpChannelFeedbackHandler.builder()
            .handleAck(() -> {
                try {
                    channel.basicAck(deliveryTag, false);
                } catch (IOException e) {
                    log.error("ack failed", e);
                    throw new RuntimeException(e);
                }
            })
            .handleNack(() -> {
                try {
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException e) {
                    log.error("nack failed", e);
                    throw new RuntimeException(e);
                }
            })
            .handNackWithRequeue(() -> {
                try {
                    channel.basicNack(deliveryTag, false, true);
                } catch (IOException e) {
                    log.error("nack with requeue failed", e);
                    throw new RuntimeException(e);
                }
            })
            .build();
    }

    private static void writeFile(InputStream encryptedAsicData, Path targetPath) {
        try {
            IOUtils.copy(encryptedAsicData, Files.newOutputStream(targetPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static UUID getDokumentlagerId(Delivery m) {
        return Optional.ofNullable(m.getProperties().getHeaders().get(DOKUMENTLAGER_PAYLOAD_TYPE_HEADER)).map(Object::toString).map(UUID::fromString).orElse(null);
    }

    private static boolean payloadInDokumentlager(Delivery m) {
        return getDokumentlagerId(m) != null;
    }

    private ConnectionFactory getConnectionFactory(AmqpKonfigurasjon amqpKonf, FiksIntegrasjonKonfigurasjon intKonf, Maskinportenklient maskinportenklient) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpKonf.getHost());
        factory.setPort(amqpKonf.getPort());
        factory.setUsername(intKonf.getIntegrasjonId().toString());
        factory.setAutomaticRecoveryEnabled(true);

        try {
            factory.useSslProtocol(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        factory.setCredentialsProvider(new CredentialsProvider() {
            @Override
            public String getUsername() {
                return intKonf.getIntegrasjonId().toString();
            }

            @Override
            public String getPassword() {
                return String.format("%s %s", intKonf.getIntegrasjonPassord(), maskinportenklient.getAccessToken(FiksIOKlientFactory.MASKINPORTEN_KS_SCOPE));
            }
        });
        return factory;
    }

    @Override
    public void close() throws IOException {
        dokumentlagerKlient.close();
    }
}
