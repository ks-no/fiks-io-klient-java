package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.ShutdownSignalException;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.NonNull;
import no.ks.fiks.dokumentlager.klient.DefaultPathHandler;
import no.ks.fiks.dokumentlager.klient.DokumentlagerHost;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.dokumentlager.klient.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import no.ks.fiks.svarinn.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.HostKonfigurasjon;
import no.ks.fiks.svarinn.client.konfigurasjon.SvarInnKonfigurasjon;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
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

        kontoId = konfigurasjon.getKontoKonfigurasjon().getKontoId();
        katalogHandler = new KatalogHandler(katalogApi);
        AsicHandler asicHandler = new AsicHandler(katalogHandler.getPublicKey(kontoId), konfigurasjon.getKontoKonfigurasjon().getPrivatNokkel(), konfigurasjon.getSigneringKonfigurasjon());
        svarInnHandler = new SvarInnHandler(kontoId, getSvarInnUtsendingKlient(konfigurasjon, maskinportenklient), katalogHandler, asicHandler);
        meldingHandler = new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(), konfigurasjon.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler, maskinportenklient, kontoId, dokumentlagerKlient);
    }

    private SvarInnUtsendingKlient getSvarInnUtsendingKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return new SvarInnUtsendingKlient(konfigurasjon.getSendMeldingKonfigurasjon().getScheme(), konfigurasjon.getSendMeldingKonfigurasjon().getHost(), konfigurasjon.getSendMeldingKonfigurasjon().getPort(), new no.ks.fiks.svarinn2.klient.IntegrasjonAuthenticationStrategy(maskinportenklient, konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(), konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()));
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

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull File payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload, @NonNull String filnavn) {
        return send(request, singletonList(new StringPayload(payload, filnavn)));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull InputStream payload, @NonNull String filanvn) {
        return send(request, singletonList(new StreamPayload(payload, filanvn)));
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

    private SvarInnKatalogApi getSvarInnKatalogApi(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return Feign.builder()
                .decoder(new JacksonDecoder(objectMapper))
                .encoder(new JacksonEncoder(objectMapper))
                .requestInterceptor(RequestInterceptors.accessToken(() -> maskinportenklient.getAccessToken("ks")))
                .requestInterceptor(RequestInterceptors.integrasjon(konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(), konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .target(SvarInnKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private DokumentlagerKlient getDokumentlagerKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return new DokumentlagerKlient(new DokumentlagerHost(konfigurasjon.getDokumentlagerKonfigurasjon().getHost(), konfigurasjon.getDokumentlagerKonfigurasjon().getPort(), konfigurasjon.getDokumentlagerKonfigurasjon().getScheme()),
                new DokumentlagerHost(konfigurasjon.getDokumentlagerKonfigurasjon().getHost(), konfigurasjon.getDokumentlagerKonfigurasjon().getPort(), konfigurasjon.getDokumentlagerKonfigurasjon().getScheme()),
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
