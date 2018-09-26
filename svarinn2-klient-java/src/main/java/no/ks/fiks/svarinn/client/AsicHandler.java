package no.ks.fiks.svarinn.client;

import lombok.NonNull;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.difi.asic.extras.CmsEncryptedAsicReader;
import no.difi.asic.extras.CmsEncryptedAsicWriter;
import no.ks.fiks.svarinn.client.model.MottattPayload;
import no.ks.fiks.svarinn.client.model.Payload;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.google.common.io.Closeables.closeQuietly;

class AsicHandler {
    private PrivateKey privatNokkel;
    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

    AsicHandler(@NonNull PrivateKey privatNokkel) {
        this.privatNokkel = privatNokkel;
    }

    File encrypt(@NonNull X509Certificate certificate, @NonNull List<Payload> payload) {
        try {
            File archiveOutputFile = File.createTempFile(UUID.randomUUID().toString(), ".asice");
            Security.addProvider(new BouncyCastleProvider());

            CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(asicWriterFactory
                    .newContainer(archiveOutputFile),
                    certificate,
                    CMSAlgorithm.AES256_GCM);

            payload.forEach(p -> write(writer, p));

            writer.setRootEntryName(payload.get(0).getFilnavn());
            writer.sign(new SignatureHelper(getClass().getResourceAsStream("/kommune1.p12"), "123456", "kommune1keyalias", "123456"));

            return archiveOutputFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<MottattPayload> decrypt(@NonNull byte[] body) {
        if (body == null || body.length == 0)
            return Collections.emptyList();

        try (AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(body))) {
            CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privatNokkel);


            List<MottattPayload> mottattPayloads = new ArrayList<>();

            while (true){
                String filnavn = reader.getNextFile();

                if (filnavn == null)
                    break;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                reader.writeFile(out);
                mottattPayloads.add(new MottattPayload(filnavn, out.toByteArray()));
            }

            return mottattPayloads;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(@NonNull CmsEncryptedAsicWriter writer, @NonNull Payload p) {
        try {
            writer.add(p.getPayload(), p.getFilnavn());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(p.getPayload());
        }

    }
}
