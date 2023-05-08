package no.ks.fiks.io.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.asice.model.KeystoreHolder;
import no.ks.fiks.io.client.konfigurasjon.*;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.client.send.FiksIOSenderClientWrapper;
import no.ks.fiks.io.klient.FiksIOUtsendingKlient;
import no.ks.fiks.maskinporten.AccessTokenRequest;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;

import java.io.IOException;
import java.util.Optional;


@Slf4j
public class FiksIOKlientFactory {

    public static final String MASKINPORTEN_KS_SCOPE = "ks:fiks";

    private final PublicKeyProvider publicKeyProvider;

    private final FiksIOKonfigurasjon fiksIOKonfigurasjon;

    public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon, @NonNull PublicKeyProvider publicKeyProvider) {
        this.fiksIOKonfigurasjon = fiksIOKonfigurasjon;
        this.publicKeyProvider = publicKeyProvider;
    }

    public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon) {
        this.fiksIOKonfigurasjon = fiksIOKonfigurasjon;
        this.publicKeyProvider = null;
    }

    /**
     * Opprett FiksIOKlient
     * @return ny {@link FiksIOKlient} instans basert på den konfigurasjonen som ble gitt
     */
    public FiksIOKlient build() {
        settDefaults(fiksIOKonfigurasjon);
        log.info("Setter opp FIKS-IO klient med følgende konfigurasjon: {}", fiksIOKonfigurasjon);

        Maskinportenklient maskinportenklient = getMaskinportenKlient(fiksIOKonfigurasjon);

        DokumentlagerKlient dokumentlagerKlient = null;
        FiksIOUtsendingKlient utsendingKlient = null;
        try {
            dokumentlagerKlient = getDokumentlagerKlient(fiksIOKonfigurasjon, maskinportenklient);
            utsendingKlient = getFiksIOUtsendingKlient(fiksIOKonfigurasjon, maskinportenklient);

            final FiksIoKatalogApi katalogApi = getFiksIOKatalogApi(fiksIOKonfigurasjon, maskinportenklient);

            AsicHandler asicHandler = AsicHandler.builder()
                .withExecutorService(fiksIOKonfigurasjon.getExecutor())
                .withPrivateNokler(fiksIOKonfigurasjon.getKontoKonfigurasjon().getPrivateNokler())
                .withKeyStoreHolder(toKeyStoreHolder(fiksIOKonfigurasjon.getVirksomhetssertifikatKonfigurasjon()))
                .build();

            KatalogHandler katalogHandler = new KatalogHandler(katalogApi);

            KontoId kontoId = fiksIOKonfigurasjon.getKontoKonfigurasjon().getKontoId();
            FiksIOHandler fiksIOHandler = new FiksIOHandler(
                kontoId,
                getFiksIOSender(utsendingKlient),
                katalogHandler, asicHandler, publicKeyProvider != null ? publicKeyProvider : new KatalogPublicKeyProvider(katalogHandler));

            return new FiksIOKlientImpl(
                kontoId,
                new AmqpHandler(fiksIOKonfigurasjon.getAmqpKonfigurasjon(),
                    fiksIOKonfigurasjon.getFiksIntegrasjonKonfigurasjon(), fiksIOHandler, asicHandler,
                    maskinportenklient, kontoId, dokumentlagerKlient),
                katalogHandler,
                fiksIOHandler
            );
        } catch (Exception e) {
            if (dokumentlagerKlient != null) {
                try {
                    dokumentlagerKlient.close();
                } catch (IOException ex) {
                    log.error("Exception under lukking av dokumentlagerklient", ex);
                }
            }
            if (utsendingKlient != null) {
                try {
                    utsendingKlient.close();
                } catch (IOException ex) {
                    log.error("Exception under lukking av utsendingsklient", ex);
                }
            }
            throw e;
        }
    }

    private static KeystoreHolder toKeyStoreHolder(VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {
        return KeystoreHolder.builder()
            .withKeyStore(virksomhetssertifikatKonfigurasjon.getKeyStore())
            .withKeyStorePassword(virksomhetssertifikatKonfigurasjon.getKeyStorePassword())
            .withKeyAlias(virksomhetssertifikatKonfigurasjon.getKeyAlias())
            .withKeyPassword(virksomhetssertifikatKonfigurasjon.getKeyPassword())
            .build();
    }

    private static FiksIOUtsendingKlient getFiksIOUtsendingKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        final SendMeldingKonfigurasjon sendMeldingKonfigurasjon = konfigurasjon.getSendMeldingKonfigurasjon();
        return FiksIOUtsendingKlient.builder()
            .withScheme(sendMeldingKonfigurasjon
                .getScheme())
            .withHostName(sendMeldingKonfigurasjon.getHost())
            .withPortNumber(sendMeldingKonfigurasjon.getPort())
            .withObjectMapper(new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
            .withAuthenticationStrategy(new no.ks.fiks.io.klient.IntegrasjonAuthenticationStrategy(maskinportenklient,
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .withRequestInterceptor(konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor())
            .build();
    }

    private static FiksIoKatalogApi getFiksIOKatalogApi(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return Feign.builder()
            .decoder(new JacksonDecoder(objectMapper))
            .encoder(new JacksonEncoder(objectMapper))
            .requestInterceptor(RequestInterceptors.accessToken(() -> maskinportenklient.getAccessToken(AccessTokenRequest.builder().scope(MASKINPORTEN_KS_SCOPE).build())))
            .requestInterceptor(RequestInterceptors.integrasjon(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .requestInterceptor(konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor() == null ? r -> {
            } : konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor())
            .target(FiksIoKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static DokumentlagerKlient getDokumentlagerKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, Maskinportenklient maskinportenklient) {
        return DokumentlagerKlient.builder()
            .api(DokumentlagerApiImpl.builder()
                .uploadBaseUrl(konfigurasjon.getDokumentlagerKonfigurasjon().getUrl())
                .downloadBaseUrl(konfigurasjon.getDokumentlagerKonfigurasjon().getUrl())
                .authenticationStrategy(
                    new IntegrasjonAuthenticationStrategy(
                        maskinportenklient,
                        konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                        konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
                .requestInterceptor(Optional.ofNullable(konfigurasjon.getDokumentlagerKonfigurasjon().getRequestInterceptor()).orElseGet(() -> r -> r))
                .build())
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static FiksIOSender getFiksIOSender(final FiksIOUtsendingKlient fiksIOUtsendingKlient) {
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