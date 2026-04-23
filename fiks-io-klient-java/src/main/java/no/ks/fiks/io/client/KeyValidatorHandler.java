package no.ks.fiks.io.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.kryptering.CMSKrypteringImpl;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;


@Slf4j
public class KeyValidatorHandler {
    private final KatalogHandler katalogHandler;
    private final KontoKonfigurasjon kontoKonfigurasjon;
    private final CMSKrypteringImpl cmsKryptering;

    private CertificateFactory cf;

    public KeyValidatorHandler(@NonNull KatalogHandler katalogHandler, @NonNull KontoKonfigurasjon kontoKonfigurasjon) {
        this.katalogHandler = katalogHandler;
        this.kontoKonfigurasjon = kontoKonfigurasjon;
        this.cmsKryptering = new CMSKrypteringImpl();
    }

    public Boolean validerOffentligNokkelMotPrivateKey() throws IllegalStateException {
        final X509Certificate publicKey = katalogHandler.getPublicKey(kontoKonfigurasjon.getKontoId());
        return validerOffentligNokkelMotPrivateKey(publicKey);
    }

    public Boolean validerOffentligNokkelMotPrivateKey(String publicKey) throws IllegalStateException {
        try {
            return validerOffentligNokkelMotPrivateKey(getPublicKeyFromString(publicKey));
        } catch (CertificateException e) {
            return false;
        }
    }

    public Boolean validerOffentligNokkelMotPrivateKey(X509Certificate publicKey) throws IllegalStateException {
        try {
            return kontoKonfigurasjon.getPrivateNokler().stream().anyMatch(privateKey ->
                krypterOgDekrypterMedNokkelparForValidering(publicKey, privateKey)
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Feil under validering av offentlig nøkkel", e);
        }
    }

    private @NonNull Boolean krypterOgDekrypterMedNokkelparForValidering(X509Certificate publicKey, PrivateKey privateKey) {
        final var byteBuffer = tilfeldigByteArray();

        try {
            final var kryptertData = cmsKryptering.krypterData(byteBuffer, publicKey);
            final var dekryptertData = cmsKryptering.dekrypterData(kryptertData, privateKey);

            return Arrays.equals(dekryptertData, byteBuffer);
        } catch (Exception exception) {
            return false;
        }
    }

    private static byte[] tilfeldigByteArray() {
        final var byteBuffer = new byte[256];
        new SecureRandom().nextBytes(byteBuffer);
        return byteBuffer;
    }

    private X509Certificate getPublicKeyFromString(@NonNull String publicKey) throws CertificateException {
        setCertificateFactoryIfNull();

        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(publicKey.getBytes()));
    }

    private void setCertificateFactoryIfNull() throws CertificateException {
        if (cf == null)  {
            cf = CertificateFactory.getInstance("X.509");
        }
    }
}