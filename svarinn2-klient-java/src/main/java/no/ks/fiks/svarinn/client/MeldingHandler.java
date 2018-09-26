package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.NonNull;
import no.ks.fiks.security.idporten.m2m.IdPortenJwtConfig;
import no.ks.fiks.security.idporten.m2m.IdPortenJwtCreater;
import no.ks.fiks.security.idporten.m2m.client.JwtAccessTokenProvider;
import no.ks.fiks.security.idporten.m2m.client.JwtResourceDetails;
import no.ks.fiks.svarinn.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.MeldingId;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;
import no.ks.fiks.svarinn2.commons.SvarInnMeldingParser;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

class MeldingHandler {

    private final Channel channel;
    private final Predicate<MeldingId> meldingErBehandlet;
    private KontoId kontoId;
    private final SvarInnHandler svarInnApi;
    private final AsicHandler asic;

    MeldingHandler(@NonNull AmqpKonfigurasjon amqpKonf,
                   @NonNull FiksIntegrasjonKonfigurasjon intKonf,
                   @NonNull SvarInnHandler svarInn,
                   @NonNull AsicHandler asic,
                   @NonNull KontoId kontoId) {
        this.svarInnApi = svarInn;
        this.asic = asic;
        this.kontoId = kontoId;
        this.meldingErBehandlet = amqpKonf.getMeldingErBehandlet();

        ConnectionFactory factory = getConnectionFactory(amqpKonf, intKonf, getOauthTemplate(intKonf));

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
        factory.setHost(amqpKonf.getHost());
        factory.setPort(amqpKonf.getPort());
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

    private OAuth2RestTemplate getOauthTemplate(FiksIntegrasjonKonfigurasjon intKonf) {
        JwtResourceDetails jwtResourceDetails = new JwtResourceDetails();

        jwtResourceDetails.setId("ID-porten");
        jwtResourceDetails.setClientId(intKonf.getIdPortenKonfigurasjon().getKlientId());
        jwtResourceDetails.setGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");
        jwtResourceDetails.setAccessTokenUri(intKonf.getIdPortenKonfigurasjon().getAccessTokenUri().toString());

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(jwtResourceDetails, new DefaultOAuth2ClientContext());

        IdPortenJwtConfig config = new IdPortenJwtConfig();
        config.setAudience("asdf");
        config.setIssuer("asdf");
        config.setScope("asdf");

        try {

            oAuth2RestTemplate.setAccessTokenProvider(new JwtAccessTokenProvider(new IdPortenJwtCreater(intKonf.getIdPortenKonfigurasjon().getPrivatNokkel(), intKonf.getIdPortenKonfigurasjon().getVirksomhetsertifikat(), config)));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        return oAuth2RestTemplate;
    }

}
