package no.ks.fiks.io.client;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.io.client.model.AmqpChannelFeedbackHandler;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.commons.FiksIOHeaders;
import no.ks.fiks.io.commons.FiksIOMeldingParser;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import no.ks.fiks.maskinporten.AccessTokenRequest;
import no.ks.fiks.maskinporten.MaskinportenklientOperations;
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
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

@Slf4j
class AmqpHandler implements Closeable {

    private static final String DOKUMENTLAGER_PAYLOAD_TYPE_HEADER = "dokumentlager-id";
    private final Channel channel;
    private final Predicate<MeldingId> meldingErBehandlet;
    private final DokumentlagerKlient dokumentlagerKlient;
    private final KontoId kontoId;
    private final FiksIOHandler fiksIOHandler;
    private final AsicHandler asicHandler;
    private final Connection amqpConnection;

    AmqpHandler(@NonNull AmqpKonfigurasjon amqpKonf,
                @NonNull FiksIntegrasjonKonfigurasjon intKonf,
                @NonNull FiksIOHandler fiksIOHandler,
                @NonNull AsicHandler asicHandler,
                @NonNull MaskinportenklientOperations maskinportenklient,
                @NonNull KontoId kontoId,
                @NonNull DokumentlagerKlient dokumentlagerKlient) {
        this.fiksIOHandler = fiksIOHandler;
        this.asicHandler = asicHandler;
        this.kontoId = kontoId;
        this.meldingErBehandlet = amqpKonf.getMeldingErBehandlet();
        this.dokumentlagerKlient = dokumentlagerKlient;

        ConnectionFactory factory = getConnectionFactory(amqpKonf, intKonf, maskinportenklient);

        try {
            amqpConnection = factory.newConnection(amqpKonf.getApplikasjonNavn());
            channel = amqpConnection.createChannel();
            channel.basicQos(amqpKonf.getMottakBufferStorrelse(), false);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void newConsume(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        try {
            channel.basicConsume(FiksIOHeaders.getKontoQueueName(kontoId.getUuid()), (ct, delivery) -> {
                MottattMeldingMetadata parsed = FiksIOMeldingParser.parse(delivery.getEnvelope(), delivery.getProperties());

                if (delivery.getEnvelope().isRedeliver() && meldingErBehandlet.test(new MeldingId(parsed.getMeldingId()))) {
                    log.debug("message {} has been delivered before and is automatically acked", parsed.getMeldingId());
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } else {

                    MottattMelding melding = getMelding(delivery, parsed, asicHandler, dokumentlagerKlient);
                    onMelding.accept(melding, fiksIOHandler.buildKvitteringSender(amqpChannelFeedbackHandler(delivery.getEnvelope().getDeliveryTag())
                        , melding));
                }
            }, (consumerTag, sig) -> onClose.accept(sig));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static MottattMelding getMelding(Delivery m, MottattMeldingMetadata parsed, AsicHandler asic, DokumentlagerKlient dokumentlagerKlient) {
        boolean hasPayloadInDokumentlager = payloadInDokumentlager(m);

        Consumer<Path> writeDekryptertZip = f -> {
            try (InputStream payload = hasPayloadInDokumentlager ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody())) {
                asic.writeDecrypted(payload, f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Consumer<Path> writeKryptertZip = f -> {
            try (InputStream payload = hasPayloadInDokumentlager ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody())) {
                writeFile(payload, f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // The consumer of these supplier methods are responsible for closing the input stream
        Supplier<InputStream> kryptertStreamSupplier = () -> hasPayloadInDokumentlager ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody());
        Supplier<ZipInputStream> zipInputStreamSupplier = () -> asic.decrypt(kryptertStreamSupplier.get());

        return MottattMelding.fromMottattMeldingMetadata(
            parsed,
            hasPayloadInDokumentlager || (m.getBody() != null && m.getBody().length > 0),
            writeDekryptertZip,
            writeKryptertZip,
            kryptertStreamSupplier,
            zipInputStreamSupplier
        );
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

    private ConnectionFactory getConnectionFactory(AmqpKonfigurasjon amqpKonf, FiksIntegrasjonKonfigurasjon intKonf, MaskinportenklientOperations maskinportenklient) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpKonf.getHost());
        factory.setPort(amqpKonf.getPort());
        factory.setUsername(intKonf.getIntegrasjonId().toString());
        factory.setAutomaticRecoveryEnabled(true);

        try {
            factory.useSslProtocol(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not setup TLS", e);
        }

        factory.setCredentialsProvider(new CredentialsProvider() {
            @Override
            public String getUsername() {
                return intKonf.getIntegrasjonId().toString();
            }

            @Override
            public String getPassword() {
                return String.format("%s %s", intKonf.getIntegrasjonPassord(), maskinportenklient.getAccessToken(AccessTokenRequest.builder().scope(FiksIOKlientFactory.MASKINPORTEN_KS_SCOPE).build()));
            }
        });
        return factory;
    }

    @Override
    public void close() throws IOException {
        dokumentlagerKlient.close();
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (TimeoutException e) {
                log.warn("Timed out while closing AMQP channel", e);
            }
        }
        if (amqpConnection.isOpen()) {
            amqpConnection.close(30_000);
        }
    }
}
