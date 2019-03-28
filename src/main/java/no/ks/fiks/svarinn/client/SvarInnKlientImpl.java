package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.ShutdownSignalException;
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
import no.ks.fiks.svarinn.client.model.FilePayload;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;
import no.ks.fiks.svarinn.client.model.MeldingRequest;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn.client.model.Payload;
import no.ks.fiks.svarinn.client.model.SendtMelding;
import no.ks.fiks.svarinn.client.model.StreamPayload;
import no.ks.fiks.svarinn.client.model.StringPayload;
import no.ks.fiks.svarinn.client.send.SvarInnSender;
import no.ks.fiks.svarinn.client.send.SvarInnSenderClientWrapper;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class SvarInnKlientImpl implements SvarInnKlient {

    private final KontoId kontoId;

    private final AmqpHandler meldingHandler;

    private final KatalogHandler katalogHandler;

    private final SvarInnHandler svarInnHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // TODO: Refaktorer til en statisk factory metode og ha en konstruktur som tar inn alle final fields. Dette vil gjøre testing enklere
    public SvarInnKlientImpl(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        settDefaults(konfigurasjon);

        Maskinportenklient maskinportenklient = getMaskinportenKlient(konfigurasjon);
        DokumentlagerKlient dokumentlagerKlient = getDokumentlagerKlient(konfigurasjon, maskinportenklient);
        SvarInnKatalogApi katalogApi = getSvarInnKatalogApi(konfigurasjon, maskinportenklient);

        kontoId = konfigurasjon.getKontoKonfigurasjon()
            .getKontoId();
        katalogHandler = new KatalogHandler(katalogApi);
        AsicHandler asicHandler = new AsicHandler(
            konfigurasjon.getKontoKonfigurasjon()
                .getPrivatNokkel(),
            konfigurasjon.getVirksomhetssertifikatKonfigurasjon());
        svarInnHandler = new SvarInnHandler(kontoId, getSvarInnSender(getSvarInnUtsendingKlient(konfigurasjon, maskinportenklient)),
            katalogHandler, asicHandler);
        meldingHandler = new AmqpHandler(konfigurasjon.getAmqpKonfigurasjon(),
            konfigurasjon.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler,
            maskinportenklient, kontoId, dokumentlagerKlient);
    }

    @Override
    public KontoId getKontoId() {
        return kontoId;
    }

    @Override
    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        return katalogHandler.lookup(request);
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return svarInnHandler.send(request, payload);
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull Path payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload, @NonNull String filnavn) {
        return send(request, singletonList(new StringPayload(payload, filnavn)));
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull InputStream payload, @NonNull String filanvn) {
        return send(request, singletonList(new StreamPayload(payload, filanvn)));
    }

    @Override
    public void newSubscription(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding) {
        newSubscription(onMelding, p -> {
        });
    }

    @Override
    public void newSubscription(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding,
                                @NonNull Consumer<ShutdownSignalException> onClose) {
        meldingHandler.newConsume(onMelding, onClose);
    }

    public void close() {
        //TODO close-it
    }

    private SvarInnUtsendingKlient getSvarInnUtsendingKlient(@NonNull SvarInnKonfigurasjon konfigurasjon,
                                                             Maskinportenklient maskinportenklient) {
        return new SvarInnUtsendingKlient(konfigurasjon.getSendMeldingKonfigurasjon().getScheme(),
            konfigurasjon.getSendMeldingKonfigurasjon().getHost(),
            konfigurasjon.getSendMeldingKonfigurasjon().getPort(),
            new no.ks.fiks.svarinn2.klient.IntegrasjonAuthenticationStrategy(
                maskinportenklient,
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()),
            konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor());
    }

    private SvarInnKatalogApi getSvarInnKatalogApi(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
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

    private DokumentlagerKlient getDokumentlagerKlient(@NonNull SvarInnKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
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

    private Maskinportenklient getMaskinportenKlient(@NonNull SvarInnKonfigurasjon konfigurasjon) {
        MaskinportenklientProperties maskinportenklientProperties = MaskinportenklientProperties.builder()
            .audience(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon()
                    .getIdPortenKonfigurasjon()
                    .getIdPortenAudience())
            .issuer(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon()
                    .getIdPortenKonfigurasjon()
                    .getKlientId())
            .numberOfSecondsLeftBeforeExpire(
                10)
            .tokenEndpoint(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon()
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

    private SvarInnSender getSvarInnSender(final SvarInnUtsendingKlient svarInnUtsendingKlient) {
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
