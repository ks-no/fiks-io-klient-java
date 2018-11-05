package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.ShutdownSignalException;
import feign.Feign;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.NonNull;
import no.ks.fiks.security.idporten.m2m.IdPortenJwtConfig;
import no.ks.fiks.security.idporten.m2m.IdPortenJwtCreater;
import no.ks.fiks.security.idporten.m2m.client.JwtAccessTokenProvider;
import no.ks.fiks.security.idporten.m2m.client.JwtResourceDetails;
import no.ks.fiks.security.idporten.m2m.feign.IdPortenFeignRequestInterceptor;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.SvarInnKonfigurasjon;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.io.Closeable;
import java.io.File;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class SvarInnKlient implements Closeable {

    private static final int DEFAULT_API_PORT = 443;
    private static final String INTEGRASJON_PASSORD_HEADER = "IntegrasjonPassord";
    private static final String INTEGRASJON_ID_HEADER = "IntegrasjonId";

    private final KontoId kontoId;
    private final AmqpHandler meldingHandler;
    private final KatalogHandler katalogHandler;
    private final SvarInnHandler svarInnHandler;

    public SvarInnKlient(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        OAuth2RestTemplate oauthTemplate = getOauthTemplate(konfigurasjon.getFiksIntegrasjonKonfigurasjon());

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SvarInnKatalogApi katalogApi = Feign.builder()
                .decoder(new JacksonDecoder(objectMapper))
                .encoder(new JacksonEncoder(objectMapper))
                .requestInterceptor(new IdPortenFeignRequestInterceptor(oauthTemplate))
                .requestInterceptor(rt -> rt
                        .header(INTEGRASJON_ID_HEADER, konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId().toString())
                        .header(INTEGRASJON_PASSORD_HEADER, konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .target(SvarInnKatalogApi.class, generateApiUri(konfigurasjon.getFiksHost(), konfigurasjon.getKatalogApiHost(), konfigurasjon.getSvarInnApiPort(), "katalogApi"));

        SvarInnApi svarInnApi = Feign.builder()
                .decoder(new JacksonDecoder(objectMapper))
                .encoder(new FormEncoder())
                .requestInterceptor(new IdPortenFeignRequestInterceptor(oauthTemplate))
                .requestInterceptor(rt -> rt
                        .header(INTEGRASJON_ID_HEADER, konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId().toString())
                        .header(INTEGRASJON_PASSORD_HEADER, konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .target(SvarInnApi.class, generateApiUri(konfigurasjon.getFiksHost(), konfigurasjon.getSvarInnApiHost(), konfigurasjon.getSvarInnApiPort(), "svarInnApi"));

        kontoId = konfigurasjon.getKontoKonfigurasjon().getKontoId();
        katalogHandler = new KatalogHandler(katalogApi);
        AsicHandler asicHandler = new AsicHandler(katalogHandler.getPublicKey(kontoId), konfigurasjon.getKontoKonfigurasjon().getPrivatNokkel(), konfigurasjon.getSigneringKonfigurasjon());
        svarInnHandler = new SvarInnHandler(kontoId, svarInnApi, katalogHandler, asicHandler);
        meldingHandler = new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(), konfigurasjon.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler, oauthTemplate, kontoId);
    }

    public KontoId getKontoId() {
        return kontoId;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        return katalogHandler.lookup(request);
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return svarInnHandler.send(request, payload);
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload) {
        return send(request, singletonList(new StringPayload(payload, "payload.txt")));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull File payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    public void newSubscription(@NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding) {
        newSubscription(onMelding, p -> {});
    }

    public void newSubscription(@NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        meldingHandler.newConsume(onMelding, onClose);
    }

    public void close(){
        //TODO close-it
    }

    private static String generateApiUri(String fiksHost, String apiHost, Integer apiPort, String apiName) {
        int port = apiPort != null ? apiPort : DEFAULT_API_PORT;
        String host = apiHost != null ? apiHost : fiksHost;

        if (host == null)
            throw new RuntimeException(String.format("fiksHost er ikke satt, men host er heller ikke satt spesifikt for %s. Sett fiksHost eller host for det spesifikke api'et", apiName));

        return host + ":" + port;
    }

    private static OAuth2RestTemplate getOauthTemplate(FiksIntegrasjonKonfigurasjon intKonf) {
        JwtResourceDetails jwtResourceDetails = new JwtResourceDetails();

        jwtResourceDetails.setId("ID-porten");
        jwtResourceDetails.setClientId(intKonf.getIdPortenKonfigurasjon().getKlientId());
        jwtResourceDetails.setGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");
        jwtResourceDetails.setAccessTokenUri(intKonf.getIdPortenKonfigurasjon().getAccessTokenUri().toString());

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(jwtResourceDetails, new DefaultOAuth2ClientContext());

        IdPortenJwtConfig config = new IdPortenJwtConfig();
        config.setAudience(intKonf.getIdPortenKonfigurasjon().getIdPortenAudience());
        config.setIssuer(intKonf.getIdPortenKonfigurasjon().getKlientId());
        config.setScope("ks");

        try {
            oAuth2RestTemplate.setAccessTokenProvider(new JwtAccessTokenProvider(new IdPortenJwtCreater(intKonf.getIdPortenKonfigurasjon().getPrivatNokkel(), intKonf.getIdPortenKonfigurasjon().getVirksomhetsertifikat(), config)));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        return oAuth2RestTemplate;
    }

}
