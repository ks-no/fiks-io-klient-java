package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

/**
 * Konfigurer fiks-integrasjonen som skal benyttes for å finne kontoer, og å sende og motta meldinger. Denne integrasjonen må være autorisert for tilgang til  kontoen over i fiks-konfigurasjon.
 */
@Value
@Builder
public class FiksIntegrasjonKonfigurasjon {

    /**
     * Påkrevd felt. Id for integrasjonen som tildelt i fiks-konfigurasjon.
     */
    @NonNull private UUID integrasjonId;


    /**
     * Påkrevd felt. Passord for integrasjonen som generert i fiks-konfigurasjon
     */
    @NonNull private String integrasjonPassord;

    /**
     * Påkrevd felt. Se {@link IdPortenKonfigurasjon}
     */
    @NonNull private IdPortenKonfigurasjon idPortenKonfigurasjon;

}
