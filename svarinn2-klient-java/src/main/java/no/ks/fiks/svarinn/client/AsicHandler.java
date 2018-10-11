package no.ks.fiks.svarinn.client;

import lombok.NonNull;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.difi.asic.extras.CmsEncryptedAsicReader;
import no.difi.asic.extras.CmsEncryptedAsicWriter;
import no.ks.fiks.svarinn.client.konfigurasjon.SigneringKonfigurasjon;
import no.ks.fiks.svarinn.client.model.MottattPayload;
import no.ks.fiks.svarinn.client.model.Payload;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.google.common.io.Closeables.closeQuietly;

class AsicHandler {
    private final PrivateKey privatNokkel;
    private final SigneringKonfigurasjon signeringKonfigurasjon;
    private final AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private final AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();
    private final byte[] keyStoreBytes;

    AsicHandler(@NonNull X509Certificate publisertOffentligNokkel, @NonNull PrivateKey privatNokkel, @NonNull SigneringKonfigurasjon signeringKonfigurasjon) {
        this.privatNokkel = privatNokkel;
        this.signeringKonfigurasjon = signeringKonfigurasjon;

        try {
        if (signeringKonfigurasjon.getKeyStore().getCertificateAlias(publisertOffentligNokkel) == null)
            throw new RuntimeException("Offentlig nøkkel publisert for denne kontoen i Fiks Konfigurasjon finnes ikke i signering-jks. For at mottaker skal kunne validere avsender må meldingen signeres med den publiserte nøkkelen.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try(ByteArrayOutputStream output = new ByteArrayOutputStream()){
            signeringKonfigurasjon.getKeyStore().store(output, signeringKonfigurasjon.getKeyStorePassword().toCharArray());
            keyStoreBytes = output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    File encrypt(@NonNull X509Certificate mottakerCert, @NonNull List<Payload> payload) {
        try {
            File archiveOutputFile = File.createTempFile(UUID.randomUUID().toString(), ".asice");
            Security.addProvider(new BouncyCastleProvider());

            CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(asicWriterFactory
                    .newContainer(archiveOutputFile),
                    mottakerCert,
                    CMSAlgorithm.AES256_GCM);
            payload.forEach(p -> write(writer, p));
            writer.setRootEntryName(payload.get(0).getFilnavn());

            try (InputStream inputStream = new ByteArrayInputStream(keyStoreBytes)) {
                writer.sign(new SignatureHelper(inputStream, signeringKonfigurasjon.getKeyStorePassword(), signeringKonfigurasjon.getKeyAlias(), signeringKonfigurasjon.getKeyPassword()));
            }

            return archiveOutputFile;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    List<MottattPayload> decrypt(@NonNull byte[] body) {
        if (body == null || body.length == 0)
            return Collections.emptyList();

        try (AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(body))) {
            CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privatNokkel);


            List<MottattPayload> mottattPayloads = new ArrayList<>();

            while (true) {
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
