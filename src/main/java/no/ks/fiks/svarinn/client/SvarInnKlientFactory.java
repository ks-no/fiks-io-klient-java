package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.NonNull;
import no.ks.fiks.dokumentlager.klient.DokumentlagerApiImpl;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.dokumentlager.klient.authentication.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import no.ks.fiks.svarinn.client.api.katalog.api.SvarInnKatalogApi;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.HostKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.SvarInnKonfigurasjon;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.send.SvarInnSender;
import no.ks.fiks.svarinn.client.send.SvarInnSenderClientWrapper;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;

public class SvarInnKlientFactory {

    public static SvarInnKlient build(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        settDefaults(konfigurasjon);

        Maskinportenklient maskinportenklient = getMaskinportenKlient(konfigurasjon);

        DokumentlagerKlient dokumentlagerKlient = getDokumentlagerKlient(konfigurasjon, maskinportenklient);

        SvarInnKatalogApi katalogApi = getSvarInnKatalogApi(konfigurasjon, maskinportenklient);

        AsicHandler asicHandler = new AsicHandler(
            konfigurasjon.getKontoKonfigurasjon().getPrivatNokkel(),
            konfigurasjon.getVirksomhetssertifikatKonfigurasjon());

        KatalogHandler katalogHandler = new KatalogHandler(katalogApi);

        KontoId kontoId = konfigurasjon.getKontoKonfigurasjon().getKontoId();

        SvarInnHandler svarInnHandler = new SvarInnHandler(
            kontoId,
            getSvarInnSender(getSvarInnUtsendingKlient(konfigurasjon, maskinportenklient)),
            katalogHandler, asicHandler);

        return new SvarInnKlientImpl(
            kontoId,
            new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler,
                maskinportenklient, kontoId, dokumentlagerKlient),
            katalogHandler,
            svarInnHandler
        );
    }

    private static SvarInnUtsendingKlient getSvarInnUtsendingKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return new SvarInnUtsendingKlient(konfigurasjon.getSendMeldingKonfigurasjon().getScheme(),
            konfigurasjon.getSendMeldingKonfigurasjon().getHost(),
            konfigurasjon.getSendMeldingKonfigurasjon().getPort(),
            new no.ks.fiks.svarinn2.klient.IntegrasjonAuthenticationStrategy(
                maskinportenklient,
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()),
            konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor());
    }

    private static SvarInnKatalogApi getSvarInnKatalogApi(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return Feign.builder()
            .decoder(new JacksonDecoder(objectMapper))
            .encoder(new JacksonEncoder(objectMapper))
            .requestInterceptor(RequestInterceptors.accessToken(() -> maskinportenklient.getAccessToken("ks")))
            .requestInterceptor(RequestInterceptors.integrasjon(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .requestInterceptor(konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor() == null ? r -> {
            } : konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor())
            .target(SvarInnKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static DokumentlagerKlient getDokumentlagerKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
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

    private static Maskinportenklient getMaskinportenKlient(@NonNull SvarInnKonfigurasjon konfigurasjon) {
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

    private static SvarInnSender getSvarInnSender(final SvarInnUtsendingKlient svarInnUtsendingKlient) {
        return new SvarInnSenderClientWrapper(svarInnUtsendingKlient);
    }

    private static void settDefaults(SvarInnKonfigurasjon konf) {
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
