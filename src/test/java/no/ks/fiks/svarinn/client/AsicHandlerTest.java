package no.ks.fiks.svarinn.client;

import com.google.common.primitives.Bytes;
import no.ks.fiks.svarinn.client.konfigurasjon.SigneringKonfigurasjon;
import no.ks.fiks.svarinn.client.model.StreamPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.support.io.TempDirectory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class AsicHandlerTest {

    @Test
    @DisplayName("Verifiser at payload blir kryptert")
    void testKrypterStream() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/" + "src/test/resources/alice-virksomhetssertifikat.p12"), "PASSWORD".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("et alias");

        AsicHandler asicHandler = new AsicHandler(cert, (PrivateKey) keyStore.getKey("et alias", "PASSWORD".toCharArray()), SigneringKonfigurasjon.builder()
                .keyAlias("et alias")
                .keyPassword("PASSWORD")
                .keyStorePassword("PASSWORD")
                .keyStore(keyStore)
                .build());

        byte[] plaintext = UUID.randomUUID().toString().getBytes();
        byte[] encrypted = readBytes(new ZipInputStream(asicHandler.encrypt(
                cert,
                singletonList(new StreamPayload(new ByteArrayInputStream(plaintext), "payload.bin")))
        )).get("payload.bin.p7m");

        //den krypterte filen skal nødvendigvis være lengre enn plaintext
        assertTrue(encrypted.length > plaintext.length);

        //verifiser at plaintext payloaden ikke finnes i den krypterte filen
        assertEquals(-1, Bytes.indexOf(encrypted, plaintext));
    }

    @Test
    @DisplayName("Test at vi kan dekryptere en payload til en zip stream")
    void testDekrypterStream() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/" + "src/test/resources/alice-virksomhetssertifikat.p12"), "PASSWORD".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("et alias");

        AsicHandler asicHandler = new AsicHandler(cert, (PrivateKey) keyStore.getKey("et alias", "PASSWORD".toCharArray()), SigneringKonfigurasjon.builder()
                .keyAlias("et alias")
                .keyPassword("PASSWORD")
                .keyStorePassword("PASSWORD")
                .keyStore(keyStore)
                .build());

        byte[] payload = UUID.randomUUID().toString().getBytes();
        InputStream encrypted = asicHandler.encrypt(cert, singletonList(new StreamPayload(new ByteArrayInputStream(payload), "payload.txt")));
        assertArrayEquals(payload, readBytes(asicHandler.decrypt(encrypted)).get("payload.txt"));
    }

    @Test
    @ExtendWith(TempDirectory.class)
    @DisplayName("Test at vi kan dekryptere en payload til en fil")
    void testDekrypterFil(@TempDirectory.TempDir Path tempDir) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/" + "src/test/resources/alice-virksomhetssertifikat.p12"), "PASSWORD".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("et alias");

        AsicHandler asicHandler = new AsicHandler(cert, (PrivateKey) keyStore.getKey("et alias", "PASSWORD".toCharArray()), SigneringKonfigurasjon.builder()
                .keyAlias("et alias")
                .keyPassword("PASSWORD")
                .keyStorePassword("PASSWORD")
                .keyStore(keyStore)
                .build());

        byte[] payload = UUID.randomUUID().toString().getBytes();
        InputStream encrypted = asicHandler.encrypt(cert, singletonList(new StreamPayload(new ByteArrayInputStream(payload), "payload.txt")));

        Path path = tempDir.resolve(UUID.randomUUID().toString());

        asicHandler.writeDecrypted(encrypted, path);
        assertTrue(Files.exists(path));
        assertArrayEquals(payload, readBytes(new ZipInputStream(Files.newInputStream(path))).get("payload.txt"));
    }

    private Map<String, byte[]> readBytes(ZipInputStream dekryptertPayload) throws IOException {
        TreeMap<String, byte[]> files = new TreeMap<>();
        ZipEntry entry;
        byte[] buffer = new byte[2048];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((entry = dekryptertPayload.getNextEntry()) != null) {
            int len;
            while ((len = dekryptertPayload.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            files.put(entry.getName(), output.toByteArray());
        }
        return files;
    }

}

