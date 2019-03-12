package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SvarInnKonfigurasjon {

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

}
