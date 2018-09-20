package no.ks.fiks.svarinn.client;

import no.difi.asic.*;
import no.difi.asic.extras.CmsEncryptedAsicReader;
import no.difi.asic.extras.CmsEncryptedAsicWriter;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class AsicHandler {
    private PrivateKey privatNokkel;
    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();

    public AsicHandler(PrivateKey privatNokkel) {
        this.privatNokkel = privatNokkel;
    }

    public File encrypt(X509Certificate certificate, InputStream inputStream) {
        try {
            File archiveOutputFile = File.createTempFile(UUID.randomUUID().toString(), ".asice");

            Security.addProvider(new BouncyCastleProvider());

            CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(AsicWriterFactory.newFactory()
                    .newContainer(archiveOutputFile),
                    certificate,
                    CMSAlgorithm.AES256_GCM);

            writer.add(inputStream, "payload.txt");
            writer.setRootEntryName("payload.txt");
           // writer.sign(new SignatureHelper(getClass().getResourceAsStream("/kommune2.p12"), "123456", "kommune2keyalias", "123456"));

            return archiveOutputFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public byte[] decrypt(byte[] body) {
        if (body == null || body.length == 0)
            return null;

        try (AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(body))) {
            CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privatNokkel);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            reader.getNextFile();
            reader.writeFile(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
