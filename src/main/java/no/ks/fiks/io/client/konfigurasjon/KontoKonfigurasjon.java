package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.io.client.model.KontoId;

import java.security.PrivateKey;
import java.util.List;

/**
 * Konfigurer Fiks IO kontoen som skal benyttes for å sende meldinger.
 */
@Value
@Builder
public class KontoKonfigurasjon {
    /**
     * Konfigurasjon for nøkler og sertifikater som skal benyttes for å signere utgående meldinger
     */
    @NonNull
    KontoId kontoId;

    /**
     * Påkrevd felt. Privat nøkkel som matcher det offentlige sertifikatet som er spesifisert for kontoen i fiks-konfigurasjon. Benyttes for å dekryptere inkommende meldinger.
     */
    @NonNull
    List<PrivateKey> privateNokler;

    @Override
    public String toString() {
        return "KontoKonfigurasjon{" +
            "kontoId=" + kontoId +
            ", privatNokkel=*****" +
            '}';
    }
}
