package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Value
@Builder
public class FiksIOKonfigurasjon {

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
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-api-host. Se {@link FiksApiKonfigurasjon}
     */
    @Builder.Default private FiksApiKonfigurasjon fiksApiKonfigurasjon = FiksApiKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-dokumentlager. Se {@link DokumentLagerKonfigurasjon}
     */
    @Builder.Default private DokumentLagerKonfigurasjon dokumentlagerKonfigurasjon = DokumentLagerKonfigurasjon.builder().build();

    /**
     * Ikke påkrevd. Kan brukes for å overkjøre defaults for fiks-svarinn-amqp. Se {@link AmqpKonfigurasjon}
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
     * Ikke påkrevd. Som standard opprettes en {@link ThreadPoolExecutor} med det antallet tråder som er angitt i {@link #DEFAULT_THREADPOOL_SIZE}
     */
    @NonNull @Builder.Default private ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREADPOOL_SIZE);

}
