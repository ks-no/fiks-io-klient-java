package no.ks.fiks.io.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.kryptering.CMSKrypteringImpl;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Random;


@Slf4j
public class KeyValidatorHandler {
    private final KatalogHandler katalogHandler;
    private final KontoKonfigurasjon kontoKonfigurasjon;
    private final CMSKrypteringImpl cmsKryptering;

    public KeyValidatorHandler(@NonNull KatalogHandler katalogHandler, KontoKonfigurasjon kontoKonfigurasjon) {
        this.katalogHandler = katalogHandler;
        this.kontoKonfigurasjon = kontoKonfigurasjon;
        this.cmsKryptering = new CMSKrypteringImpl();
    }

    public Boolean validerOffentligNokkelMotPrivateKey() {
        try {
            final X509Certificate publicKey = katalogHandler.getPublicKey(kontoKonfigurasjon.getKontoId());
            final PrivateKey privateKey = kontoKonfigurasjon.getPrivateNokler().get(0);

            final var byteBuffer = tilfeldigByteArray();

            try {
                final var kryptertData = cmsKryptering.krypterData(byteBuffer, publicKey);
                final var dekryptertData = cmsKryptering.dekrypterData(kryptertData, privateKey);

                return Arrays.equals(dekryptertData, byteBuffer);
            } catch (Exception exception) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Feil under validering av offentlig nøkkel", e);
        }
    }

    private static byte[] tilfeldigByteArray() {
        final var byteBuffer = new byte[256];
        new Random().nextBytes(byteBuffer);
        return byteBuffer;
    }
}