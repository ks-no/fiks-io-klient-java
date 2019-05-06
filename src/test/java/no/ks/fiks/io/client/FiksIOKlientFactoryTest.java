package no.ks.fiks.io.client;

import lombok.NonNull;
import no.ks.fiks.io.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.IdPortenKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.io.client.model.KontoId;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.Test;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FiksIOKlientFactoryTest {

    @Test
    void build() {
        final KontoKonfigurasjon kontoKonfigurasjon = KontoKonfigurasjon.builder()
                                                                        .kontoId(new KontoId(UUID.randomUUID()))
                                                                        .privatNokkel(generatePrivateKey())
                                                                        .build();
        final VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon = VirksomhetssertifikatKonfigurasjon.builder()
                                                                                                                        .keyStorePassword("PASSWORD")
                                                                                                                        .keyStore(
                                                                                                                            readAliceVirksomhetssertifikat())
                                                                                                                        .keyAlias("et alias")
                                                                                                                        .keyPassword("PASSWORD")
                                                                                                                        .build();
        final IdPortenKonfigurasjon idPortenKonfigurasjon = IdPortenKonfigurasjon.builder()
                                                                                 .accessTokenUri("http://localhost")
                                                                                 .idPortenAudience("audience")
                                                                                 .klientId(UUID.randomUUID()
                                                                                               .toString())
                                                                                 .build();
        final FiksIntegrasjonKonfigurasjon fiksIntegrasjonKonfigurasjon = FiksIntegrasjonKonfigurasjon.builder()
                                                                                                      .integrasjonId(UUID.randomUUID())
                                                                                                      .integrasjonPassord(UUID.randomUUID()
                                                                                                                              .toString())
                                                                                                      .idPortenKonfigurasjon(idPortenKonfigurasjon)
                                                                                                      .build();
        final FiksApiKonfigurasjon fiksApiKonfigurasjon = FiksApiKonfigurasjon.builder()
                                                                              .host("localhost")
                                                                              .port(9190)
                                                                              .build();
        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
                                                                           .kontoKonfigurasjon(kontoKonfigurasjon)
                                                                           .virksomhetssertifikatKonfigurasjon(
                                                                                  virksomhetssertifikatKonfigurasjon)
                                                                           .fiksIntegrasjonKonfigurasjon(fiksIntegrasjonKonfigurasjon)
                                                                           .fiksApiKonfigurasjon(fiksApiKonfigurasjon)
                                                                           .build();
        assertThrows(RuntimeException.class, () -> FiksIOKlientFactory.build(fiksIOKonfigurasjon));

    }

    private static PrivateKey generatePrivateKey() {
        try (final InputStream keyStream = FiksIOKlientFactoryTest.class.getResourceAsStream("/alice.key");
             final Reader keyReader = new InputStreamReader(keyStream, StandardCharsets.UTF_8)) {
            final Object pemKeyInfo = new PEMParser(keyReader).readObject();
            return KeyFactory.getInstance("RSA")
                             .generatePrivate(
                                 new PKCS8EncodedKeySpec(((PrivateKeyInfo) pemKeyInfo).getEncoded()));

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Kunne ikke generere privatnøkkel", e);
        }
    }

    private static KeyStore readAliceVirksomhetssertifikat() {
        try (final InputStream inputStream = FiksIOKlientFactoryTest.class.getResourceAsStream("/" + "alice-virksomhetssertifikat.p12")) {
            return readP12(inputStream, "PASSWORD");
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke laste virksomhetssertifikat", e);
        }
    }

    private static KeyStore readP12(@NonNull InputStream p12inputStream, @NonNull String keystorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(p12inputStream, keystorePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}