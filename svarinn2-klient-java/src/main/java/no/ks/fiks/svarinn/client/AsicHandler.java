package no.ks.fiks.svarinn.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.difi.asic.extras.CmsEncryptedAsicReader;
import no.difi.asic.extras.CmsEncryptedAsicWriter;
import no.ks.fiks.svarinn.client.konfigurasjon.SigneringKonfigurasjon;
import no.ks.fiks.svarinn.client.model.Payload;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Closeables.closeQuietly;

@Slf4j
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

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            signeringKonfigurasjon.getKeyStore().store(output, signeringKonfigurasjon.getKeyStorePassword().toCharArray());
            keyStoreBytes = output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    InputStream encrypt(@NonNull X509Certificate mottakerCert, @NonNull List<Payload> payload) {

        try {
            if (payload.isEmpty())
                throw new RuntimeException("Ingen payloads oppgitt, kan ikke kryptere melding");

            PipedInputStream inputStream = new PipedInputStream();
            final OutputStream outputStream = new PipedOutputStream(inputStream);

            new Thread(() -> {
                try {
                    Security.addProvider(new BouncyCastleProvider());

                    CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(
                            asicWriterFactory.newContainer(outputStream),
                            mottakerCert,
                            CMSAlgorithm.AES256_GCM);
                    payload.forEach(p -> write(writer, p));
                    writer.setRootEntryName(payload.get(0).getFilnavn());

                    try (InputStream keyStoreStream = new ByteArrayInputStream(keyStoreBytes)) {
                        writer.sign(new SignatureHelper(keyStoreStream, signeringKonfigurasjon.getKeyStorePassword(), signeringKonfigurasjon.getKeyAlias(), signeringKonfigurasjon.getKeyPassword()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            return inputStream;
        } catch (IOException e) {
            throw  new RuntimeException();
        }
    }

    public ZipInputStream decrypt(@NonNull InputStream encryptedAsicData) {
        try {
            PipedOutputStream out = new PipedOutputStream();

            new Thread(() -> {
                System.out.println("start decrypting");
                try (AsicReader asicReader = asicReaderFactory.open(encryptedAsicData)) {
                    asicReader.getAsicManifest()
                    decrypt(asicReader, new ZipOutputStream(out));
                } catch (Exception e) {
                    log.error("", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.error("", e);
                    }
                    System.out.println("done decrypting");
                }

            }).start();

            return new ZipInputStream(new PipedInputStream(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDecrypted(@NonNull InputStream encryptedAsicData, @NonNull Path targetPath) {
        try (AsicReader asicReader = asicReaderFactory.open(encryptedAsicData);
             OutputStream out1 = Files.newOutputStream(targetPath);
             ZipOutputStream out = new ZipOutputStream(out1)) {
            decrypt(asicReader, out);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private void decrypt(AsicReader asicReader, ZipOutputStream zipOutputStream) throws IOException {
        try (CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privatNokkel)) {
        while (true) {
                String filnavn = reader.getNextFile();

                if (filnavn == null)
                    break;

                zipOutputStream.putNextEntry(new ZipEntry(filnavn));
                reader.writeFile(zipOutputStream);
                zipOutputStream.closeEntry();
                zipOutputStream.finish();
            }
        }
    }

    private void write(@NonNull CmsEncryptedAsicWriter writer, @NonNull Payload p) {
        try {
            writer.addEncrypted(p.getPayload(), p.getFilnavn());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(p.getPayload());
        }

    }
}
