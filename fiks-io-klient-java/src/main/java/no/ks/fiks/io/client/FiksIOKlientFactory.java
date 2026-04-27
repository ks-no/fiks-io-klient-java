package no.ks.fiks.io.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.FeignException;
import feign.hc5.ApacheHttp5Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.DokumentlagerApiImpl;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;
import no.ks.fiks.fiksio.client.api.konfigurasjon.api.FiksIoKontoApi;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.asice.model.KeystoreHolder;
import no.ks.fiks.io.client.konfigurasjon.*;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.client.send.FiksIOSenderClientWrapper;
import no.ks.fiks.io.klient.FiksIOUtsendingKlient;
import no.ks.fiks.io.klient.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.maskinporten.AccessTokenRequestBuilder;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.VersionInfo;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
public class FiksIOKlientFactory {

    public static final String MASKINPORTEN_KS_SCOPE = "ks:fiks";

    private final PublicKeyProvider publicKeyProvider;

    private final FiksIOKonfigurasjon fiksIOKonfigurasjon;
    private Supplier<String> maskinportenAccessTokenSupplier;

    private final CloseableHttpClient httpClient;

    public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon, PublicKeyProvider publicKeyProvider, @NonNull CloseableHttpClient httpClient) {
        this.fiksIOKonfigurasjon = fiksIOKonfigurasjon;
        this.publicKeyProvider = publicKeyProvider;
        this.httpClient = httpClient;
    }

    public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon, PublicKeyProvider publicKeyProvider) {
        this(fiksIOKonfigurasjon, publicKeyProvider, httpClient());
    }

    public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon) {
        this(fiksIOKonfigurasjon, null);
    }

    public FiksIOKlientFactory setMaskinportenAccessTokenSupplier(Supplier<String> maskinportenAccessTokenSupplier) {
        this.maskinportenAccessTokenSupplier = maskinportenAccessTokenSupplier;
        return this;
    }

    /**
     * Opprett FiksIOKlient
     *
     * @return ny {@link FiksIOKlient} instans basert på den konfigurasjonen som ble gitt
     */
    public FiksIOKlient build() {
        settDefaults(fiksIOKonfigurasjon);
        final var executor = fiksIOKonfigurasjon.getExecutor();
        log.info("Setter opp FIKS-IO klient med følgende konfigurasjon: {}", fiksIOKonfigurasjon);


        if (this.maskinportenAccessTokenSupplier == null) {
            Maskinportenklient maskinportenKlient = getMaskinportenKlient(fiksIOKonfigurasjon);
            this.maskinportenAccessTokenSupplier = () -> maskinportenKlient.getAccessToken(new AccessTokenRequestBuilder().scope(MASKINPORTEN_KS_SCOPE).build());
        }

        DokumentlagerKlient dokumentlagerKlient = null;
        FiksIOUtsendingKlient utsendingKlient = null;

        try {
            dokumentlagerKlient = getDokumentlagerKlient(fiksIOKonfigurasjon, maskinportenAccessTokenSupplier);
            utsendingKlient = getFiksIOUtsendingKlient(fiksIOKonfigurasjon, httpClient, maskinportenAccessTokenSupplier);

            final AsicHandler asicHandler = AsicHandler.builder()
                .withExecutorService(executor)
                .withPrivateNokler(getPrivateNokler())
                .withKeyStoreHolder(toKeyStoreHolder(fiksIOKonfigurasjon.getVirksomhetssertifikatKonfigurasjon()))
                .build();

            final KatalogHandler katalogHandler = new KatalogHandler(
                getFiksIOKatalogApi(fiksIOKonfigurasjon, maskinportenAccessTokenSupplier, httpClient),
                getPublicFiksIOKatalogApi(fiksIOKonfigurasjon, httpClient),
                getFiksIOKontoApi(fiksIOKonfigurasjon, maskinportenAccessTokenSupplier, httpClient));

            final KontoId kontoId = fiksIOKonfigurasjon.getKontoKonfigurasjon().getKontoId();
            final FiksIOHandler fiksIOHandler = new FiksIOHandler(
                kontoId,
                getFiksIOSender(utsendingKlient),
                asicHandler, publicKeyProvider != null ? publicKeyProvider : new KatalogPublicKeyProvider(katalogHandler));

            final KeyValidatorHandler keyValidatorHandler = new KeyValidatorHandler(katalogHandler, fiksIOKonfigurasjon.getKontoKonfigurasjon());

            final FiksIOKlient klient =  new FiksIOKlientImpl(
                kontoId,
                new AmqpHandler(fiksIOKonfigurasjon.getAmqpKonfigurasjon(),
                    fiksIOKonfigurasjon.getFiksIntegrasjonKonfigurasjon(), fiksIOHandler, asicHandler,
                    maskinportenAccessTokenSupplier, kontoId, dokumentlagerKlient),
                katalogHandler,
                fiksIOHandler,
                keyValidatorHandler,
                executor
            );

            lastOppOffentligNokkelHvisOppdatert(katalogHandler, keyValidatorHandler, kontoId);

            return klient;
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

    private void lastOppOffentligNokkelHvisOppdatert(KatalogHandler katalogHandler, KeyValidatorHandler keyValidatorHandler, KontoId kontoId) {
        final String publicKey = fiksIOKonfigurasjon.getKontoKonfigurasjon().getPublicKey();

        if(publicKey == null)
            return;

        if(offentligNokkelUlikFraFiksIOKatalog(katalogHandler, kontoId, publicKey)) {
            if(keyValidatorHandler.validerOffentligNokkelMotPrivateKey(publicKey)) {
                lastOppOffentligNokkel(katalogHandler, kontoId, publicKey);
            } else {
                throw new RuntimeException("Offentlignøkkel kan ikke valideres opp mot konfigurerte private nøkler");
            }
        }
    }

    private static Boolean offentligNokkelUlikFraFiksIOKatalog(KatalogHandler katalogHandler, KontoId kontoId, String publicKey) {
        try {
            X509Certificate publicKeyFraKatalog = katalogHandler.getPublicKey(kontoId);

            if(publicKeyFraKatalog == null) {
                return true;
            }

            return !publicKey.lines().collect(Collectors.joining()).contains(Base64.getEncoder().encodeToString(publicKeyFraKatalog.getEncoded()));
        } catch (FeignException.NotFound | CertificateException e) {
            return true;
        }
    }

    private void lastOppOffentligNokkel(KatalogHandler katalogHandler, KontoId kontoId, String publicKey) {
        try {
            if(publicKey == null) {
                throw new RuntimeException("Offentlignøkkel mangler i konfigurasjon, og finnes ikke i katalogen. Kan ikke fortsette uten offentlignøkkel.");
            }

            katalogHandler.uploadPublicKey(kontoId, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Feil med opplasting av public key", e);
        }
    }

    private @NonNull List<PrivateKey> getPrivateNokler() {
        return fiksIOKonfigurasjon.getKontoKonfigurasjon().getPrivateNokler();
    }

    private static KeystoreHolder toKeyStoreHolder(VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {
        return KeystoreHolder.builder()
            .withKeyStore(virksomhetssertifikatKonfigurasjon.getKeyStore())
            .withKeyStorePassword(virksomhetssertifikatKonfigurasjon.getKeyStorePassword())
            .withKeyAlias(virksomhetssertifikatKonfigurasjon.getKeyAlias())
            .withKeyPassword(virksomhetssertifikatKonfigurasjon.getKeyPassword())
            .build();
    }

    private static FiksIOUtsendingKlient getFiksIOUtsendingKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, final CloseableHttpClient httpClient, Supplier<String> maskinportenAccessTokenSupplier) {
        final SendMeldingKonfigurasjon sendMeldingKonfigurasjon = konfigurasjon.getSendMeldingKonfigurasjon();
        return FiksIOUtsendingKlient.builder()
            .withHttpClient(httpClient)
            .withScheme(sendMeldingKonfigurasjon
                .getScheme())
            .withHostName(sendMeldingKonfigurasjon.getHost())
            .withPortNumber(sendMeldingKonfigurasjon.getPort())
            .withObjectMapper(new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
            .withAuthenticationStrategy(
                new IntegrasjonAuthenticationStrategy(
                    maskinportenAccessTokenSupplier,
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                    konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .withRequestInterceptor(konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor() == null ? r -> r : konfigurasjon.getSendMeldingKonfigurasjon().getRequestInterceptor())
            .build();
    }

    private static FiksIoKatalogApi getFiksIOKatalogApi(@NonNull FiksIOKonfigurasjon konfigurasjon, Supplier<String> maskinportenAccessTokenSupplier, final CloseableHttpClient httpClient) {
        return baseKatalogApiKlientBuilder(konfigurasjon, httpClient)
            .requestInterceptor(RequestInterceptors.accessToken(maskinportenAccessTokenSupplier))
            .requestInterceptor(RequestInterceptors.integrasjon(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .target(FiksIoKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static FiksIoKontoApi getFiksIOKontoApi(@NonNull FiksIOKonfigurasjon konfigurasjon, Supplier<String> maskinportenAccessTokenSupplier, final CloseableHttpClient httpClient) {
        return baseKatalogApiKlientBuilder(konfigurasjon, httpClient)
            .requestInterceptor(RequestInterceptors.accessToken(maskinportenAccessTokenSupplier))
            .requestInterceptor(RequestInterceptors.integrasjon(
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId(),
                konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord()))
            .target(FiksIoKontoApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static FiksIoKatalogApi getPublicFiksIOKatalogApi(FiksIOKonfigurasjon konfigurasjon, final CloseableHttpClient httpClient) {
        return baseKatalogApiKlientBuilder(konfigurasjon, httpClient).target(FiksIoKatalogApi.class, konfigurasjon.getKatalogKonfigurasjon().getUrl());
    }

    private static Feign.Builder baseKatalogApiKlientBuilder(FiksIOKonfigurasjon konfigurasjon, final CloseableHttpClient httpClient) {
        final var objectMapper = getObjectMapper();
        return Feign.builder()
            .client(new ApacheHttp5Client(httpClient))
            .decoder(new JacksonDecoder(objectMapper))
            .encoder(new JacksonEncoder(objectMapper))
            .requestInterceptor(konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor() == null ? r -> {
            } : konfigurasjon.getKatalogKonfigurasjon().getRequestInterceptor());

    }

    private static CloseableHttpClient httpClient() {
        return HttpClients.custom()
            .useSystemProperties()
            .evictIdleConnections(TimeValue.of(Duration.ofMinutes(1L)))
            .setUserAgent("fiks-io-klient %s %s".formatted(Meta.VERSJON, VersionInfo.getSoftwareInfo("Apache-HttpClient",
                "org.apache.hc.client5", HttpClientBuilder.class)))
            .build();
    }

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }


    private static DokumentlagerKlient getDokumentlagerKlient(@NonNull FiksIOKonfigurasjon konfigurasjon, Supplier<String> maskinportenAccessTokenSupplier) {
        return DokumentlagerKlient.builder()
            .api(DokumentlagerApiImpl.builder()
                .uploadBaseUrl(konfigurasjon.getDokumentlagerKonfigurasjon().getUrl())
                .downloadBaseUrl(konfigurasjon.getDokumentlagerKonfigurasjon().getUrl())
                .authenticationStrategy(request ->
                    request.headers(c ->
                        c.add("Authorization", "Bearer " + maskinportenAccessTokenSupplier.get())
                            .add("IntegrasjonId", konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonId().toString())
                            .add("IntegrasjonPassord", konfigurasjon.getFiksIntegrasjonKonfigurasjon().getIntegrasjonPassord())))
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
            final var keyStore = konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyStore();
            return Maskinportenklient.builder()
                .withPrivateKey((PrivateKey) keyStore.getKey(konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyAlias(), konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyPassword().toCharArray()))
                .usingVirksomhetssertifikat((X509Certificate) keyStore.getCertificate(konfigurasjon.getVirksomhetssertifikatKonfigurasjon().getKeyAlias()))
                .withProperties(maskinportenklientProperties)
                .build();
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