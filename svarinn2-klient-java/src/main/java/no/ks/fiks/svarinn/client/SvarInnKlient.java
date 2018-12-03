package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.ShutdownSignalException;
import feign.Feign;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.dokumentlager.klient.*;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import no.ks.fiks.svarinn.client.konfigurasjon.*;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.io.Closeable;
import java.io.File;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class SvarInnKlient implements Closeable {

    private final KontoId kontoId;
    private final AmqpHandler meldingHandler;
    private final KatalogHandler katalogHandler;
    private final SvarInnHandler svarInnHandler;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public SvarInnKlient(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        settDefaults(konfigurasjon);

        Maskinportenklient maskinportenklient = getMaskinportenKlient(konfigurasjon);
        DokumentlagerKlient dokumentlagerKlient = getDokumentlagerKlient(konfigurasjon, maskinportenklient);
        SvarInnKatalogApi katalogApi = getSvarInnKatalogApi(konfigurasjon, maskinportenklient);
        SvarInnApi svarInnApi = getSvarInnApi(konfigurasjon, maskinportenklient);

        kontoId = konfigurasjon.getKontoKonfigurasjon().getKontoId();
        katalogHandler = new KatalogHandler(katalogApi);
        AsicHandler asicHandler = new AsicHandler(katalogHandler.getPublicKey(kontoId), konfigurasjon.getKontoKonfigurasjon().getPrivatNokkel(), konfigurasjon.getSigneringKonfigurasjon());
        svarInnHandler = new SvarInnHandler(kontoId, svarInnApi, katalogHandler, asicHandler);
        meldingHandler = new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(), konfigurasjon.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler, maskinportenklient, kontoId, dokumentlagerKlient);
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

    private SvarInnApi getSvarInnApi(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return Feign.builder()
                .decoder(new JacksonDecoder(objectMapper))
                .encoder(new FormEncoder())
                .requestInterceptor(RequestInterceptors.settAccessToken(maskinportenklient, "ks"))
                .requestInterceptor(RequestInterceptors.settIntegrasjon(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(), konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .target(SvarInnApi.class, konfigurasjon.getSendMeldingKonfigurasjon().getUrl());
    }

    private SvarInnKatalogApi getSvarInnKatalogApi(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return Feign.builder()
                .decoder(new JacksonDecoder(objectMapper))
                .encoder(new JacksonEncoder(objectMapper))
                .requestInterceptor(RequestInterceptors.settAccessToken(maskinportenklient, "ks"))
                .requestInterceptor(RequestInterceptors.settIntegrasjon(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(), konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .target(SvarInnKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private DokumentlagerKlient getDokumentlagerKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        DokumentlagerUploadProperties dokumentlagerUploadProperties = new DokumentlagerUploadProperties();
        dokumentlagerUploadProperties.setHost(konfigurasjon.getDokumentlagerKonfigurasjon().getHost());
        dokumentlagerUploadProperties.setPort(konfigurasjon.getDokumentlagerKonfigurasjon().getPort());
        dokumentlagerUploadProperties.setScheme(konfigurasjon.getDokumentlagerKonfigurasjon().getScheme());

        DokumentlagerDownloadProperties dokumentlagerDownloadProperties = new DokumentlagerDownloadProperties();
        dokumentlagerDownloadProperties.setHost(konfigurasjon.getDokumentlagerKonfigurasjon().getHost());
        dokumentlagerDownloadProperties.setPort(konfigurasjon.getDokumentlagerKonfigurasjon().getPort());
        dokumentlagerDownloadProperties.setScheme(konfigurasjon.getDokumentlagerKonfigurasjon().getScheme());
        return new DokumentlagerKlient(
                dokumentlagerUploadProperties,
                dokumentlagerDownloadProperties,
                new IntegrasjonAuthenticationStrategy(
                        maskinportenklient,
                        konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                        konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()),
                new DefaultPathHandler()
        );
    }

    private Maskinportenklient getMaskinportenKlient(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        MaskinportenklientProperties maskinportenklientProperties = MaskinportenklientProperties.builder()
                .audience(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIdPortenKonfigurasjon().getIdPortenAudience())
                .issuer(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIdPortenKonfigurasjon().getKlientId())
                .numberOfSecondsLeftBeforeExpire(10)
                .tokenEndpoint(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIdPortenKonfigurasjon().getAccessTokenUri())
                .build();

        try {
            return new Maskinportenklient(
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIdPortenKonfigurasjon().getPrivatNokkel(),
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIdPortenKonfigurasjon().getVirksomhetsertifikat(),
                    maskinportenklientProperties);
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void settDefaults(SvarInnKonfigurasjon konf) {
        FiksApiKonfigurasjon fiksKonf = konf.getFiksApiKonfigurasjon();
        settDefaults(fiksKonf, konf.getDokumentlagerKonfigurasjon());
        settDefaults(fiksKonf, konf.getKatalogKonfigurasjon());
        settDefaults(fiksKonf, konf.getSendMeldingKonfigurasjon());

        if (konf.getAmqpKonfigurasjon().getHost() == null)
            konf.getAmqpKonfigurasjon().setHost(fiksKonf.getHost());
    }

    private static void settDefaults(FiksApiKonfigurasjon fiksKonf, HostKonfigurasjon hostKonf) {
        if (hostKonf.getHost() == null)
            hostKonf.setHost(fiksKonf.getHost());
        if (hostKonf.getPort() == null)
            hostKonf.setPort(fiksKonf.getPort());
        if (hostKonf.getScheme() == null)
            hostKonf.setScheme(fiksKonf.getScheme());
    }
}
