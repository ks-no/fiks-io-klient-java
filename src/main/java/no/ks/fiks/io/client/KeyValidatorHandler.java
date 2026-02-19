package no.ks.fiks.io.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.kryptering.CMSKrypteringImpl;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;


@Slf4j
public class KeyValidatorHandler {
    private final KatalogHandler katalogHandler;
    private final KontoKonfigurasjon kontoKonfigurasjon;
    private final CMSKrypteringImpl cmsKryptering;

    public KeyValidatorHandler(@NonNull KatalogHandler katalogHandler, @NonNull KontoKonfigurasjon kontoKonfigurasjon) {
        this.katalogHandler = katalogHandler;
        this.kontoKonfigurasjon = kontoKonfigurasjon;
        this.cmsKryptering = new CMSKrypteringImpl();
    }

    public Boolean validerOffentligNokkelMotPrivateKey() throws IllegalStateException {
        try {
            final X509Certificate publicKey = katalogHandler.getPublicKey(kontoKonfigurasjon.getKontoId());
            final int antallPrivateNokler = kontoKonfigurasjon.getPrivateNokler().size();

            if(antallPrivateNokler != 1) {
                throw new IllegalStateException(String.format("Forventet nøyaktig én privat nøkkel, konto har %d private nøkler", antallPrivateNokler));
            }

            final PrivateKey privateKey = kontoKonfigurasjon.getPrivateNokler().get(0);

            final var byteBuffer = tilfeldigByteArray();

            try {
                final var kryptertData = cmsKryptering.krypterData(byteBuffer, publicKey);
                final var dekryptertData = cmsKryptering.dekrypterData(kryptertData, privateKey);

                return Arrays.equals(dekryptertData, byteBuffer);
            } catch (Exception exception) {
                return false;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Feil under validering av offentlig nøkkel", e);
        }
    }

    private static byte[] tilfeldigByteArray() {
        final var byteBuffer = new byte[256];
        new SecureRandom().nextBytes(byteBuffer);
        return byteBuffer;
    }
}