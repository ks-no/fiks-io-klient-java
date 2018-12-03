package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Konfigurasjon for integrasjonens autentisering via virksomhetssertifikat basert token fra ID-Porten
 */
@Value
@Builder
public class IdPortenKonfigurasjon {
    /**
     * Påkrevd. Virksomhetsertifikatet som skal brukes for å autentisere mot ID-Porten under henting av OAuth access token
     */
    @NonNull private X509Certificate virksomhetsertifikat;

    /**
     * Privat nøkkel som matcher virksomhetsertifikatet over.
     */
    @NonNull private PrivateKey privatNokkel;

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
