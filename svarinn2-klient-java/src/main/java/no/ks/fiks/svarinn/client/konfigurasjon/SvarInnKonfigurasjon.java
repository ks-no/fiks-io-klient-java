package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;

import java.util.function.Function;

@Value
@Builder
public class SvarInnKonfigurasjon {

    /**
     * Påkrevd felt. Host-navn for ks-fiks.
     */
    @NonNull private String fiksHost;

    /**
     * Påkrevd felt. Se {@link KontoKonfigurasjon}
     */
    @NonNull private KontoKonfigurasjon kontoKonfigurasjon;

    /**
     * Påkrevd felt. Se {@link SigneringKonfigurasjon}
     */
    @NonNull private SigneringKonfigurasjon signeringKonfigurasjon;

    /**
     * Påkrevd felt. Se {@link FiksIntegrasjonKonfigurasjon}
     */
    @NonNull private FiksIntegrasjonKonfigurasjon fiksIntegrasjonKonfigurasjon;

    /**
     * Ikke påkrevd. Se {@link AmqpKonfigurasjon}
     */
    private AmqpKonfigurasjon amqpKonfigurasjon;

    /**
     * Ikke påkrevd. Kan brukes til å overstyre {@link SvarInnKonfigurasjon#fiksHost} for dette endepunktet.
     */
    private String katalogApiHost;

    /**
     * Ikke påkrevd. Kan brukes til å overstyre default port for dette endepunktet.
     */
    private Integer katalogApiPort;

    /**
     * Ikke påkrevd. Gir mulighet til å benytte en cache for oppslag mot svarinn katalogen for å bedre ytelse. Hvis dette ikke settes vil ikke caching bli benyttet
     */
    private Function<LookupRequest, KontoId> katalogApiCache;

     /**
      * Ikke påkrevd. Kan brukes til å overstyre {@link SvarInnKonfigurasjon#fiksHost} for dette endepunktet.
      */
    private String svarInnApiHost;

    /**
     * Ikke påkrevd. Kan brukes til å overstyre default port for dette endepunktet.
     */
    private Integer svarInnApiPort;
}
