package no.ks.fiks.io.client;

import lombok.NonNull;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class TestUtil {
    public static PrivateKey generatePrivateKey() {
        try (final InputStream keyStream = TestUtil.class.getResourceAsStream("/alice.key");
             final Reader keyReader = new InputStreamReader(keyStream, StandardCharsets.UTF_8)) {
            final Object pemKeyInfo = new PEMParser(keyReader).readObject();
            return KeyFactory.getInstance("RSA")
                             .generatePrivate(
                                 new PKCS8EncodedKeySpec(((PrivateKeyInfo) pemKeyInfo).getEncoded()));

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Kunne ikke generere privatnøkkel", e);
        }
    }

    public static KeyStore readAliceVirksomhetssertifikat() {
        try (final InputStream inputStream = TestUtil.class.getResourceAsStream("/" + "alice-virksomhetssertifikat.p12")) {
            return readP12(inputStream, "PASSWORD");
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke laste virksomhetssertifikat", e);
        }
    }

    public static KeyStore readP12(@NonNull InputStream p12inputStream, @NonNull String keystorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(p12inputStream, keystorePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
