package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.KeyStore;

/**
 * Konfigurasjon for nøkler og sertifikater som skal benyttes for å signere utgående meldinger. Merk at nøkkel som benyttes ved signering ikke nødvendigvis er den samme som den som benyttes ved dekryptering.
 */
@Value
@Builder
public class VirksomhetssertifikatKonfigurasjon {
    /**
     * Påkrevd felt. Java key-store med avsenders offentlige sertifikat og CA kjede, og matchende privat nøkkel
     */
    @NonNull KeyStore keyStore;

    /**
     * Påkrevd felt. Passord for keyStore over.
     */
    @NonNull String keyStorePassword;

    /**
     * Påkrevd felt. Alias for privat nøkkel i keyStore over.
     */
    @NonNull String keyAlias;

    /**
     * Påkrevd felt. Passord for privat nøkkel i keyStore over.
     */
    @NonNull String keyPassword;

    @Override
    public String toString() {
        return "VirksomhetssertifikatKonfigurasjon{" +
            "keyStore=" + keyStore +
            ", keyStorePassword='*****'" +
            ", keyAlias='" + keyAlias + '\'' +
            ", keyPassword='*****'" +
            '}';
    }
}
