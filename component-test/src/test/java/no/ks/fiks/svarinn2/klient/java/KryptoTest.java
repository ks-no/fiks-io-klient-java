
package no.ks.fiks.svarinn2.klient.java;

import no.difi.asic.*;
import no.difi.asic.extras.CmsEncryptedAsicReader;
import no.difi.asic.extras.CmsEncryptedAsicWriter;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

public class KryptoTest {
    @Test
    void name() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        // Creates an ASiC archive after which every entry is read back from the archive.

// Name of the file to hold the the ASiC archive
        File archiveOutputFile = new File(System.getProperty("java.io.tmpdir"), "asic-sample-default.asice");
        Security.addProvider(new BouncyCastleProvider());
        // WRITE TO ASIC
        KeyStore keyStore = loadKeyStore("kommune1.p12", "123456");
        System.out.println(keyStore.aliases().nextElement());
        // Fetching certificate
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("kommune1keyalias");

        // Store result in ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Create a new ASiC archive
        AsicWriter asicWriter = AsicWriterFactory.newFactory().newContainer(byteArrayOutputStream);
        // Encapsulate ASiC archive to enable writing encrypted content
        CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(asicWriter, certificate, CMSAlgorithm.AES256_GCM);

        writer.addEncrypted(Thread.currentThread().getContextClassLoader().getResourceAsStream("small.pdf"), "small.pdf", MimeType.forString("application/pdf"));

        writer.setRootEntryName("small.pdf");
        writer.sign(new SignatureHelper(getClass().getResourceAsStream("/kommune2.p12"), "123456", "kommune2keyalias", "123456"));

        // READ FROM ASIC

        // Fetch private key from keystore
        PrivateKey privateKey = (PrivateKey) keyStore.getKey("kommune1keyalias", "123456".toCharArray());

        // Open content of ByteArrayOutputStream for reading
        AsicReader asicReader = AsicReaderFactory.newFactory().open(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        // Encapsulate ASiC archive to enable reading encrypted content
        CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privateKey);


        // Read file
        assertEquals(reader.getNextFile(), "small.pdf");
        ByteArrayOutputStream file1 = new ByteArrayOutputStream();
        reader.writeFile(file1);

        // Verify no more files are found
        assertNull(reader.getNextFile());

        // Verify certificate used for signing of ASiC is the same as the one used for signing
        //assertEquals(reader.getAsicManifest().getCertificate().get(0).getCertificate(), certificate.getEncoded());

        assertEquals(reader.getAsicManifest().getRootfile(), "small.pdf");

        asicReader.close();

        assertArrayEquals(IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("small.pdf")), file1.toByteArray());


    }

    private KeyStore loadKeyStore(String name, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // Read JKS
        KeyStore keyStore = KeyStore.getInstance("JKS");
        final InputStream resourceAsStream = getClass().getResourceAsStream("/" + name);
        keyStore.load(resourceAsStream, password.toCharArray());
        return keyStore;
    }
}

