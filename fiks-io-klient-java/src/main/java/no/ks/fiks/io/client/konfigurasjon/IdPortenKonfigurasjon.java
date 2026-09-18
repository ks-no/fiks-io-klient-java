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
    @NonNull
    private String accessTokenUri;

    /**
     * Påkrevd. Aud-claim for request tokens mot ID-Porten
     */
    @NonNull
    private String idPortenAudience;

    /**
     * Påkrevd. Organisasjonens oauth klient-id, som registrert hos ID-Porten.
     */
    @NonNull
    private String klientId;

    /**
     * Valgfritt. Settes som JWT {@code kid}-header ved bruk av asymmetrisk nøkkel for
     * Maskinporten-autentisering. Når denne er satt benyttes asymmetrisk nøkkel
     * (se {@link AsymmetriskNokkelKonfigurasjon}) i stedet for virksomhetssertifikat for autentisering.
     */
    private String keyIdentifier;

    public static class IdPortenKonfigurasjonBuilder {
    }

    ;

    /**
     * Ny builder med ferdig prod config. Trenger klientId. Returnerer en frisk builder for hvert kall.
     */
    public static IdPortenKonfigurasjonBuilder prod() {
        return IdPortenKonfigurasjon.builder()
            .accessTokenUri("https://maskinporten.no/token")
            .idPortenAudience("https://maskinporten.no/");
    }

    /**
     * Ny builder med ferdig TEST config. Kan brukes mot Fiks IO sitt testmiljø. Trenger klientId.
     * Returnerer en frisk builder for hvert kall.
     */
    public static IdPortenKonfigurasjonBuilder test() {
        return IdPortenKonfigurasjon.builder()
            .accessTokenUri("https://test.maskinporten.no/token")
            .idPortenAudience("https://test.maskinporten.no/");
    }

    /**
     * Builder med ferdig prod config. Trenger klientId.
     *
     * @deprecated Dette er en delt, muterbar builder-instans. Verdier satt på den (f.eks. {@code keyIdentifier})
     * lekker mellom kall i samme JVM. Bruk {@link #prod()} som returnerer en frisk builder for hvert kall.
     */
    @Deprecated
    public static final IdPortenKonfigurasjonBuilder PROD = IdPortenKonfigurasjon.builder()
        .accessTokenUri("https://maskinporten.no/token")
        .idPortenAudience("https://maskinporten.no/");

    /**
     * Builder med ferdig ver2 config. Trenger klientId. Brukes mot Fiks IO i test.
     *
     * @deprecated Bruk {@link #test()} i stedet. Digdir holder på å fase ut VER2
     */
    @Deprecated(since = "3.3.0", forRemoval = true)
    public static final IdPortenKonfigurasjonBuilder VER2 = IdPortenKonfigurasjon.builder()
        .accessTokenUri("https://oidc-ver2.difi.no/idporten-oidc-provider/token")
        .idPortenAudience("https://oidc-ver2.difi.no/idporten-oidc-provider/");

    /**
     * Builder med ferdig TEST config. Kan brukes mot Fiks IO sitt testmiljø. Trenger klientId.
     *
     * @deprecated Dette er en delt, muterbar builder-instans. Verdier satt på den (f.eks. {@code keyIdentifier})
     * lekker mellom kall i samme JVM. Bruk {@link #test()} som returnerer en frisk builder for hvert kall.
     */
    @Deprecated
    public static final IdPortenKonfigurasjonBuilder TEST = IdPortenKonfigurasjon.builder()
        .accessTokenUri("https://test.maskinporten.no/token")
        .idPortenAudience("https://test.maskinporten.no/");
}
