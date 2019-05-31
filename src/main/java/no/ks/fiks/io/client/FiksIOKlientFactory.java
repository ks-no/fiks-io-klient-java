package no.ks.fiks.io.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.DokumentlagerApiImpl;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.dokumentlager.klient.authentication.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.io.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.HostKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.SendMeldingKonfigurasjon;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.client.send.FiksIOSenderClientWrapper;
import no.ks.fiks.io.klient.FiksIOUtsendingKlient;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import no.ks.fiks.svarinn.client.api.katalog.api.FiksIoKatalogApi;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;

@Slf4j
public class FiksIOKlientFactory {

    public static final String MASKINPORTEN_KS_SCOPE = "ks:fiks";

    public static FiksIOKlient build(@NonNull FiksIOKonfigurasjon konfigurasjon) {
        settDefaults(konfigurasjon);
        log.info("Setter opp FIKS-IO klient med følgende konfigurasjon: {}", konfigurasjon);

        Maskinportenklient maskinportenklient = getMaskinportenKlient(konfigurasjon);

        DokumentlagerKlient dokumentlagerKlient = getDokumentlagerKlient(konfigurasjon, maskinportenklient);

        final FiksIoKatalogApi katalogApi = getFiksIOKatalogApi(konfigurasjon, maskinportenklient);

        AsicHandler asicHandler = new AsicHandler(
            konfigurasjon.getKontoKonfigurasjon().getPrivatNokkel(),
            konfigurasjon.getVirksomhetssertifikatKonfigurasjon(), konfigurasjon.getExecutor());

        KatalogHandler katalogHandler = new KatalogHandler(katalogApi);

        KontoId kontoId = konfigurasjon.getKontoKonfigurasjon().getKontoId();

        FiksIOHandler fiksIOHandler = new FiksIOHandler(
            kontoId,
            getSvarInnSender(getSvarInnUtsendingKlient(konfigurasjon, maskinportenklient)),
            katalogHandler, asicHandler);

        return new FiksIOKlientImpl(
            kontoId,
            new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon(), fiksIOHandler, asicHandler,
                maskinportenklient, kontoId, dokumentlagerKlient),
            katalogHandler,
            fiksIOHandler
        );
    }

    private static FiksIOUtsendingKlient getSvarInnUtsendingKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        final SendMeldingKonfigurasjon sendMeldingKonfigurasjon = konfigurasjon.getSendMeldingKonfigurasjon();
        return FiksIOUtsendingKlient.builder()
                                    .withScheme(sendMeldingKonfigurasjon
                                                             .getScheme())
            .withHostName(sendMeldingKonfigurasjon.getHost())
            .withPortNumber(sendMeldingKonfigurasjon.getPort())
            .withObjectMapper(new ObjectMapper().findAndRegisterModules())
            .withAuthenticationStrategy(new no.ks.fiks.io.klient.IntegrasjonAuthenticationStrategy(maskinportenklient,
                                                                                                   konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                                                                                                   konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .withRequestInterceptor(konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor())
            .build();
    }

    private static FiksIoKatalogApi getFiksIOKatalogApi(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return Feign.builder()
            .decoder(new JacksonDecoder(objectMapper))
            .encoder(new JacksonEncoder(objectMapper))
            .requestInterceptor(RequestInterceptors.accessToken(() -> maskinportenklient.getAccessToken(MASKINPORTEN_KS_SCOPE)))
            .requestInterceptor(RequestInterceptors.integrasjon(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .requestInterceptor(konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor() == null ? r -> {
            } : konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor())
            .target(FiksIoKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static DokumentlagerKlient getDokumentlagerKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return DokumentlagerKlient.builder()
            .api(new DokumentlagerApiImpl(
                konfigurasjon.getDokumentlagerKonfigurasjon().getUrl(),
                konfigurasjon.getDokumentlagerKonfigurasjon().getUrl(),
                new IntegrasjonAuthenticationStrategy(
                    maskinportenklient,
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()),
                konfigurasjon.getDokumentlagerKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getDokumentlagerKonfigurasjon().getRequestInterceptor()))
            .build();
    }

    private static Maskinportenklient getMaskinportenKlient(@NonNull FiksIOKonfigurasjon konfigurasjon) {
        MaskinportenklientProperties maskinportenklientProperties = MaskinportenklientProperties.builder()
            .audience(konfigurasjon.getFiksIntegrasjonKonfigurasjon()
                    .getIdPortenKonfigurasjon()
                    .getIdPortenAudience())
            .issuer(konfigurasjon.getFiksIntegrasjonKonfigurasjon()
                    .getIdPortenKonfigurasjon()
                    .getKlientId())
            .numberOfSecondsLeftBeforeExpire(10)
            .tokenEndpoint(konfigurasjon.getFiksIntegrasjonKonfigurasjon()
                    .getIdPortenKonfigurasjon()
                    .getAccessTokenUri())
            .build();

        try {
            return new Maskinportenklient(
                konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyStore(),
                konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyAlias(),
                konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyPassword().toCharArray(),
                maskinportenklientProperties);
        } catch (CertificateEncodingException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static FiksIOSender getSvarInnSender(final FiksIOUtsendingKlient fiksIOUtsendingKlient) {
        return new FiksIOSenderClientWrapper(fiksIOUtsendingKlient);
    }

    private static void settDefaults(FiksIOKonfigurasjon konf) {
        FiksApiKonfigurasjon fiksKonf = konf.getFiksApiKonfigurasjon();
        settDefaults(fiksKonf, konf.getDokumentlagerKonfigurasjon());
        settDefaults(fiksKonf, konf.getKatalogKonfigurasjon());
        settDefaults(fiksKonf, konf.getSendMeldingKonfigurasjon());

        if (konf.getAmqpKonfigurasjon()
            .getHost() == null) {
            konf.getAmqpKonfigurasjon()
                .setHost(fiksKonf.getHost());
        }
    }

    private static void settDefaults(FiksApiKonfigurasjon fiksKonf, HostKonfigurasjon hostKonf) {
        if (hostKonf.getHost() == null) {
            hostKonf.setHost(fiksKonf.getHost());
        }
        if (hostKonf.getPort() == null) {
            hostKonf.setPort(fiksKonf.getPort());
        }
        if (hostKonf.getScheme() == null) {
            hostKonf.setScheme(fiksKonf.getScheme());
        }
    }
}