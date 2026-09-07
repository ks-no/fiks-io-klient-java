package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.PrivateKey;

/**
 * Konfigurasjon for asymmetrisk nøkkel som benyttes til Maskinporten-autentisering, som et alternativ
 * til {@link VirksomhetssertifikatKonfigurasjon}. Brukes sammen med {@code keyIdentifier} på
 * {@link IdPortenKonfigurasjon}, som settes som JWT {@code kid}-header.
 * <p>
 * Merk at ASiC-E-signering fortsatt krever et eget X.509-sertifikat
 * (se {@link VirksomhetssertifikatKonfigurasjon}).
 */
@Value
@Builder
public class AsymmetriskNokkelKonfigurasjon {

    /**
     * Påkrevd. Privat nøkkel som er registrert i Maskinporten for asymmetrisk nøkkel-autentisering.
     */
    @NonNull PrivateKey privatNokkel;

    @Override
    public String toString() {
        return "AsymmetriskNokkelKonfigurasjon{privatNokkel=*****}";
    }
}
