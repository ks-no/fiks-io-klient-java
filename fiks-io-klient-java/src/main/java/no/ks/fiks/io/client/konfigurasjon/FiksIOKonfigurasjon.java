package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Value
@Builder
/**
 * Konfigurasjon for {@link no.ks.fiks.io.client.FiksIOKlient}
 */
public class FiksIOKonfigurasjon {

    /**
     * Standard størrelse på trådpool
     */
    public static final int DEFAULT_THREADPOOL_SIZE = 5;

    /**
     * Påkrevd felt. Se {@link KontoKonfigurasjon}
     */
    @NonNull private KontoKonfigurasjon kontoKonfigurasjon;

    /**
     * Påkrevd felt. Se {@link VirksomhetssertifikatKonfigurasjon}
     */
    @NonNull private VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon;

    /**
     * Påkrevd felt. Se {@link FiksIntegrasjonKonfigurasjon}
     */
    @NonNull private FiksIntegrasjonKonfigurasjon fiksIntegrasjonKonfigurasjon;

    /**
     * Ikke påkrevd. Asymmetrisk nøkkel som benyttes til Maskinporten-autentisering, som et alternativ
     * til {@link VirksomhetssertifikatKonfigurasjon}. Brukes sammen med {@code keyIdentifier} på
     * {@link IdPortenKonfigurasjon}. Se {@link AsymmetriskNokkelKonfigurasjon}.
     */
    private AsymmetriskNokkelKonfigurasjon asymmetriskNokkelKonfigurasjon;

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-api-host. Se {@link FiksApiKonfigurasjon}
     */
    @Builder.Default private FiksApiKonfigurasjon fiksApiKonfigurasjon = FiksApiKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-dokumentlager. Se {@link DokumentLagerKonfigurasjon}
     */
    @Builder.Default private DokumentLagerKonfigurasjon dokumentlagerKonfigurasjon = DokumentLagerKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-io-amqp. Se {@link AmqpKonfigurasjon}
     */
    @Builder.Default private AmqpKonfigurasjon amqpKonfigurasjon = AmqpKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-katalog. Se {@link KatalogKonfigurasjon}
     */
    @Builder.Default private KatalogKonfigurasjon katalogKonfigurasjon = KatalogKonfigurasjon.builder().build();

     /**
      * Ikke påkrevd. Kan brukes for å overkjøre defaults for endepunkt for å sende meldinger til SvarInn. Se {@link SendMeldingKonfigurasjon}
      */
    @Builder.Default private SendMeldingKonfigurasjon sendMeldingKonfigurasjon = SendMeldingKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Som standard opprettes en {@link ThreadPoolExecutor} med det antallet tråder som er angitt i {@link #DEFAULT_THREADPOOL_SIZE}. Denne benyttes til kryptering av data som sendes med meldingen.
     */
    @NonNull @Builder.Default private ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREADPOOL_SIZE);

    public static FiksIOKonfigurasjon defaultProdConfiguration(
        final String klientId,
        final UUID integrasjonId,
        final String integrasjonPassord,
        final KontoKonfigurasjon kontoKonfigurasjon,
        final VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {

        return FiksIOKonfigurasjon.builder()
            .fiksApiKonfigurasjon(FiksApiKonfigurasjon.PROD)
            .amqpKonfigurasjon(AmqpKonfigurasjon.PROD)
            .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                .idPortenKonfigurasjon(IdPortenKonfigurasjon.PROD
                    .klientId(klientId)
                    .build())
                .integrasjonId(integrasjonId)
                .integrasjonPassord(integrasjonPassord)
                .build())
            .kontoKonfigurasjon(kontoKonfigurasjon)
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon)
            .build();
    }

    public static FiksIOKonfigurasjon defaultTestConfiguration(
        final String klientId,
        final UUID integrasjonId,
        final String integrasjonPassord,
        final KontoKonfigurasjon kontoKonfigurasjon,
        final VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {

        return FiksIOKonfigurasjon.builder()
            .fiksApiKonfigurasjon(FiksApiKonfigurasjon.TEST)
            .amqpKonfigurasjon(AmqpKonfigurasjon.TEST)
            .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                .idPortenKonfigurasjon(IdPortenKonfigurasjon.TEST
                    .klientId(klientId)
                    .build())
                .integrasjonId(integrasjonId)
                .integrasjonPassord(integrasjonPassord)
                .build())
            .kontoKonfigurasjon(kontoKonfigurasjon)
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon)
            .build();
    }

    /**
     * Oppretter prod-konfigurasjon som autentiserer mot Maskinporten med asymmetrisk nøkkel
     * (privat nøkkel + {@code keyIdentifier}) i stedet for virksomhetssertifikat.
     * {@code virksomhetssertifikatKonfigurasjon} kreves fortsatt for ASiC-E-signering.
     */
    public static FiksIOKonfigurasjon defaultProdConfiguration(
        final String klientId,
        final UUID integrasjonId,
        final String integrasjonPassord,
        final KontoKonfigurasjon kontoKonfigurasjon,
        final VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon,
        final String keyIdentifier,
        final AsymmetriskNokkelKonfigurasjon asymmetriskNokkelKonfigurasjon) {

        return FiksIOKonfigurasjon.builder()
            .fiksApiKonfigurasjon(FiksApiKonfigurasjon.PROD)
            .amqpKonfigurasjon(AmqpKonfigurasjon.PROD)
            .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                .idPortenKonfigurasjon(IdPortenKonfigurasjon.PROD
                    .klientId(klientId)
                    .keyIdentifier(keyIdentifier)
                    .build())
                .integrasjonId(integrasjonId)
                .integrasjonPassord(integrasjonPassord)
                .build())
            .kontoKonfigurasjon(kontoKonfigurasjon)
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon)
            .asymmetriskNokkelKonfigurasjon(asymmetriskNokkelKonfigurasjon)
            .build();
    }

    /**
     * Oppretter test-konfigurasjon som autentiserer mot Maskinporten med asymmetrisk nøkkel
     * (privat nøkkel + {@code keyIdentifier}) i stedet for virksomhetssertifikat.
     * {@code virksomhetssertifikatKonfigurasjon} kreves fortsatt for ASiC-E-signering.
     */
    public static FiksIOKonfigurasjon defaultTestConfiguration(
        final String klientId,
        final UUID integrasjonId,
        final String integrasjonPassord,
        final KontoKonfigurasjon kontoKonfigurasjon,
        final VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon,
        final String keyIdentifier,
        final AsymmetriskNokkelKonfigurasjon asymmetriskNokkelKonfigurasjon) {

        return FiksIOKonfigurasjon.builder()
            .fiksApiKonfigurasjon(FiksApiKonfigurasjon.TEST)
            .amqpKonfigurasjon(AmqpKonfigurasjon.TEST)
            .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                .idPortenKonfigurasjon(IdPortenKonfigurasjon.TEST
                    .klientId(klientId)
                    .keyIdentifier(keyIdentifier)
                    .build())
                .integrasjonId(integrasjonId)
                .integrasjonPassord(integrasjonPassord)
                .build())
            .kontoKonfigurasjon(kontoKonfigurasjon)
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon)
            .asymmetriskNokkelKonfigurasjon(asymmetriskNokkelKonfigurasjon)
            .build();
    }

}
