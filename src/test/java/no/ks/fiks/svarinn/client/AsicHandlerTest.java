package no.ks.fiks.svarinn.client;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.svarinn.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.svarinn.client.model.StreamPayload;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AsicHandlerTest {

    @Test
    @DisplayName("Verifiser at payload blir kryptert")
    void testKrypterStream() throws Exception {
        KeyStore keyStore = getKeyStore();

        AsicHandler asicHandler = new AsicHandler(getPrivateKeyResource("/bob.key"), VirksomhetssertifikatKonfigurasjon.builder()
            .keyAlias("et alias")
            .keyPassword("PASSWORD")
            .keyStorePassword("PASSWORD")
            .keyStore(keyStore)
            .build());

        byte[] plaintext = UUID.randomUUID().toString().getBytes();
        InputStream encrypt = asicHandler.encrypt(
            getPublicCertResource("bob.cert"),
            singletonList(new StreamPayload(new ByteArrayInputStream(plaintext), "payload.bin")));
        log.info("started reading");
        byte[] encrypted = IOUtils.toByteArray(encrypt);
        log.info("done reading");
        encrypt.close();

        //den krypterte filen skal nødvendigvis være lengre enn plaintext
        assertTrue(encrypted.length > plaintext.length);

        //verifiser at plaintext payloaden ikke finnes i den krypterte filen
        assertEquals(-1, Bytes.indexOf(encrypted, plaintext));
    }

    @Test
    @DisplayName("Test at vi kan dekryptere en payload til en zip stream")
    void testDekrypterStream() throws Exception {
        KeyStore keyStore = getKeyStore();

        AsicHandler asicHandler = new AsicHandler(getPrivateKeyResource("/bob.key"), VirksomhetssertifikatKonfigurasjon.builder()
            .keyAlias("et alias")
            .keyPassword("PASSWORD")
            .keyStorePassword("PASSWORD")
            .keyStore(keyStore)
            .build());

        byte[] payload = UUID.randomUUID().toString().getBytes();
        InputStream encrypted = asicHandler.encrypt(getPublicCertResource("bob.cert"),  singletonList(new StreamPayload(new ByteArrayInputStream(payload), "payload.txt")));
        ZipInputStream decrypt = asicHandler.decrypt(encrypted);
        assertArrayEquals(payload, readBytes(decrypt).get("payload.txt"));
        decrypt.close();
        encrypted.close();
    }

    @Test
    @DisplayName("Test at vi kan dekryptere en payload til en fil")
    void testDekrypterFil(@TempDir Path tempDir) throws Exception {
        KeyStore keyStore = getKeyStore();

        AsicHandler asicHandler = new AsicHandler(getPrivateKeyResource("/bob.key"), VirksomhetssertifikatKonfigurasjon.builder()
            .keyAlias("et alias")
            .keyPassword("PASSWORD")
            .keyStorePassword("PASSWORD")
            .keyStore(keyStore)
            .build());

        byte[] payload = UUID.randomUUID().toString().getBytes();
        InputStream encrypted = asicHandler.encrypt(getPublicCertResource("bob.cert"), singletonList(new StreamPayload(new ByteArrayInputStream(payload), "payload.txt")));

        Path path = tempDir.resolve(UUID.randomUUID().toString());

        asicHandler.writeDecrypted(encrypted, path);
        assertTrue(Files.exists(path));
        assertArrayEquals(payload, readBytes(new ZipInputStream(Files.newInputStream(path))).get("payload.txt"));
    }

    @Test
    @DisplayName("Test at vi kan dekryptere mange streams samtidig")
    void testDekrypterStreamMultiThread() throws Exception {
        KeyStore keyStore = getKeyStore();

        AsicHandler asicHandler = new AsicHandler(getPrivateKeyResource("/bob.key"), VirksomhetssertifikatKonfigurasjon.builder()
            .keyAlias("et alias")
            .keyPassword("PASSWORD")
            .keyStorePassword("PASSWORD")
            .keyStore(keyStore)
            .build());

        int threads = 30;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean();
        AtomicInteger overlaps = new AtomicInteger();

        Collection<CompletableFuture<Boolean>> futures =
            new ArrayList<>(threads);

        for (int t = 0; t < threads; ++t) {
            futures.add(CompletableFuture.supplyAsync(
                () -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (running.get()) {
                        overlaps.incrementAndGet();
                    }
                    running.set(true);
                    byte[] payload = UUID.randomUUID().toString().getBytes();

                    try {
                        InputStream encrypt = asicHandler.encrypt(getPublicCertResource("bob.cert"), singletonList(new StreamPayload(new ByteArrayInputStream(payload), "payload.txt")));
                        ZipInputStream decrypt = asicHandler.decrypt(new ByteArrayInputStream(IOUtils.toByteArray(encrypt)));
                        log.info("test thread done");
                        boolean arrayEquals = Arrays.equals(payload, readBytes(decrypt).get("payload.txt"));
                        running.set(false);
                        return arrayEquals;
                    } catch (IOException e) {
                        throw new RuntimeException(); }
                }
            ));
        }

        latch.countDown();

        assertTrue(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(future -> futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList())).get().stream()
            .allMatch(p -> p));

        assertTrue(overlaps.get() > 0);
    }

    private KeyStore getKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(this.getClass().getClassLoader().getResourceAsStream("alice-virksomhetssertifikat.p12"), "PASSWORD".toCharArray());
        return keyStore;
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

    private X509Certificate getPublicCertResource(String filename) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            return (X509Certificate) fact.generateCertificate(new FileInputStream("src/test/resources/"+ filename));
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private PrivateKey getPrivateKeyResource(String filename) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            PrivateKeyInfo o = (PrivateKeyInfo) new PEMParser(new StringReader(IOUtils.resourceToString(filename, Charset.defaultCharset()))).readObject();
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(o.getEncoded()));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}

