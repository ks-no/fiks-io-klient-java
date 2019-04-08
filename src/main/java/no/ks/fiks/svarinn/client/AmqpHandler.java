package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.NonNull;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.svarinn.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.MeldingId;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;
import no.ks.fiks.svarinn2.commons.SvarInn2Headers;
import no.ks.fiks.svarinn2.commons.SvarInnMeldingParser;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

class AmqpHandler {

    private static final String DOKUMENTLAGER_PAYLOAD_TYPE_HEADER = "DOKUMENTLAGER_PAYLOAD";
    private final Channel channel;
    private final Predicate<MeldingId> meldingErBehandlet;
    private final DokumentlagerKlient dokumentlagerKlient;
    private KontoId kontoId;
    private final SvarInnHandler svarInnApi;
    private final AsicHandler asic;

    AmqpHandler(@NonNull AmqpKonfigurasjon amqpKonf,
                @NonNull FiksIntegrasjonKonfigurasjon intKonf,
                @NonNull SvarInnHandler svarInn,
                @NonNull AsicHandler asic,
                @NonNull Maskinportenklient maskinportenklient,
                @NonNull KontoId kontoId,
                @NonNull DokumentlagerKlient dokumentlagerKlient) {
        this.svarInnApi = svarInn;
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
            channel.basicConsume(SvarInn2Headers.getKontoQueueName(kontoId.getUuid()), (ct, m) -> {
                MottattMeldingMetadata parsed = SvarInnMeldingParser.parse(m.getEnvelope(), m.getProperties());

                if (m.getEnvelope().isRedeliver() && meldingErBehandlet.test(new MeldingId(parsed.getMeldingId()))) {
                    channel.basicAck(m.getEnvelope().getDeliveryTag(), false);
                } else {
                    MottattMelding melding = MottattMelding.fromMottattMeldingMetadata(
                            parsed,
                            f -> asic.writeDecrypted(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()), f),
                            f -> writeFile(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()), f),
                            () -> payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody()),
                            () -> asic.decrypt(payloadInDokumentlager(m) ? dokumentlagerKlient.download(getDokumentlagerId(m)).getResult() : new ByteArrayInputStream(m.getBody())));
                    onMelding.accept(melding, svarInnApi.buildKvitteringSender(() -> {
                        try {
                            channel.basicAck(m.getEnvelope().getDeliveryTag(), false);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, melding));
                }
            }, (consumerTag, sig) -> onClose.accept(sig));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(InputStream encryptedAsicData, Path targetPath) {
        try {
            IOUtils.copy(encryptedAsicData, Files.newOutputStream(targetPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static UUID getDokumentlagerId(Delivery m) {
        return UUID.fromString((String) m.getProperties().getHeaders().get(DOKUMENTLAGER_PAYLOAD_TYPE_HEADER));
    }

    private static boolean payloadInDokumentlager(Delivery m) {
        return m.getProperties().getHeaders().containsKey(DOKUMENTLAGER_PAYLOAD_TYPE_HEADER);
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
                return String.format("%s %s", intKonf.getIntegrasjonPassord(), maskinportenklient.getAccessToken(SvarInnKlientFactory.MASKINPORTEN_KS_SCOPE));
            }
        });
        return factory;
    }

}
