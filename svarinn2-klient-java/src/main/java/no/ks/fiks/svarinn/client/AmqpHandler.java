package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.MeldingId;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;
import no.ks.fiks.svarinn2.commons.SvarInnMeldingParser;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

class AmqpHandler {

    private final Channel channel;
    private final Predicate<MeldingId> meldingErBehandlet;
    private KontoId kontoId;
    private final SvarInnHandler svarInnApi;
    private final AsicHandler asic;

    AmqpHandler(@NonNull AmqpKonfigurasjon amqpKonf,
                @NonNull FiksIntegrasjonKonfigurasjon intKonf,
                @NonNull SvarInnHandler svarInn,
                @NonNull AsicHandler asic,
                @NonNull OAuth2RestTemplate oAuth2RestTemplate,
                @NonNull KontoId kontoId) {
        this.svarInnApi = svarInn;
        this.asic = asic;
        this.kontoId = kontoId;
        this.meldingErBehandlet = amqpKonf.getMeldingErBehandlet();

        ConnectionFactory factory = getConnectionFactory(amqpKonf, intKonf, oAuth2RestTemplate);

        try {
            channel = factory.newConnection().createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void newConsume(@NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        try {
            channel.basicConsume(kontoId.toString(), (ct, m) -> {
                MottattMeldingMetadata parsed = SvarInnMeldingParser.parse(m.getEnvelope(), m.getProperties());

                if (m.getEnvelope().isRedeliver() && meldingErBehandlet.test(new MeldingId(parsed.getMeldingId()))) {
                    channel.basicAck(m.getEnvelope().getDeliveryTag(), false);
                } else {
                    MottattMelding melding = MottattMelding.fromMottattMeldingMetadata(parsed, asic.decrypt(m.getBody()));
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

    private ConnectionFactory getConnectionFactory(AmqpKonfigurasjon amqpKonf, FiksIntegrasjonKonfigurasjon intKonf, OAuth2RestTemplate oAuth2RestTemplate) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpKonf.getAmqpHost());
        factory.setPort(amqpKonf.getAmqpPort());
        factory.setUsername(intKonf.getIntegrasjonId().toString());
        factory.setAutomaticRecoveryEnabled(true);

        factory.setCredentialsProvider(new CredentialsProvider() {
            @Override
            public String getUsername() {
                return intKonf.getIntegrasjonId().toString();
            }

            @Override
            public String getPassword() {
                return String.format("%s %s", intKonf.getIntegrasjonPassord(), oAuth2RestTemplate.getAccessToken().getValue());
            }
        });
        return factory;
    }

}
