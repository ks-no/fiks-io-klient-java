package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Konfigurasjon for integrasjonens autentisering via virksomhetssertifikat basert token fra ID-Porten
 */
@Value
@Builder
public class IdPortenKonfigurasjon {

    /**
     * Påkrevd. Uri som skal benyttes for å utføre token-request mot ID-Porten.
     */
    @NonNull private String accessTokenUri;

    /**
     * Påkrevd. Aud-claim for request tokens mot ID-Porten
     */
    @NonNull private String idPortenAudience;

    /**
     * Påkrevd. Organisasjonens oauth klient-id, som registrert hos ID-Porten.
     */
    @NonNull private String klientId;

}
