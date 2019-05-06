package no.ks.fiks.io.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.difi.asic.SignatureMethod;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.io.client.model.Payload;
import no.ks.kryptering.CMSKrypteringImpl;
import no.ks.kryptering.CMSStreamKryptering;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Closeables.closeQuietly;

@Slf4j
class AsicHandler {
    private final PrivateKey privatNokkel;
    private final VirksomhetssertifikatKonfigurasjon signeringKonfigurasjon;
    private final AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory(SignatureMethod.CAdES);
    private final AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory(SignatureMethod.CAdES);
    private final ExecutorService executor;
    private final byte[] keyStoreBytes;

    AsicHandler(@NonNull final PrivateKey privatNokkel,
                @NonNull final VirksomhetssertifikatKonfigurasjon signeringKonfigurasjon,
                @NonNull final ExecutorService executor) {
        this.privatNokkel = privatNokkel;
        this.signeringKonfigurasjon = signeringKonfigurasjon;
        this.executor = executor;
        Security.addProvider(new BouncyCastleProvider());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            signeringKonfigurasjon.getKeyStore().store(output, signeringKonfigurasjon.getKeyStorePassword().toCharArray());
            keyStoreBytes = output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    InputStream encrypt(@NonNull final X509Certificate mottakerCert, @NonNull final List<Payload> payload) {
        try {
            if (payload.isEmpty())
                throw new RuntimeException("Ingen payloads oppgitt, kan ikke kryptere melding");

            PipedInputStream asicInputStream = new PipedInputStream();
            final OutputStream asicOutputStream = new PipedOutputStream(asicInputStream);
            executor.submit(() -> {
                try {
                    AsicWriter writer = asicWriterFactory.newContainer(asicOutputStream);
                    payload.forEach(p -> write(writer, p));
                    writer.setRootEntryName(payload.get(0)
                                                   .getFilnavn());
                    try (InputStream keyStoreStream = new ByteArrayInputStream(keyStoreBytes)) {
                        writer.sign(
                            new SignatureHelper(keyStoreStream, signeringKonfigurasjon.getKeyStorePassword(), signeringKonfigurasjon.getKeyAlias(),
                                                signeringKonfigurasjon.getKeyPassword()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    log.info("asic builder thread dead");
                }
            });


            PipedInputStream kryptertInputStream = new PipedInputStream();
            final PipedOutputStream kryptertOutputStream = new PipedOutputStream(kryptertInputStream);


            executor.submit(() -> {
                CMSStreamKryptering cmsKryptoHandler = new CMSKrypteringImpl();
                try (OutputStream krypteringStream = cmsKryptoHandler.getKrypteringOutputStream(kryptertOutputStream, mottakerCert)){
                    IOUtils.copy(asicInputStream, krypteringStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        asicOutputStream.close();
                        asicInputStream.close();
                        kryptertOutputStream.close();
                    } catch (IOException e) {
                        log.error("Uventet feil under cleanup", e);
                    }
                }
            });

            return kryptertInputStream;
        } catch (IOException e) {
            throw new RuntimeException("Feil under bygging av asic", e);
        }
    }

    public ZipInputStream decrypt(@NonNull InputStream encryptedAsicData) {
        try {

            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream pipedInputStream = new PipedInputStream(out);

            executor.execute(() -> {
                ZipOutputStream zipOutputStream = new ZipOutputStream(out);
                decrypt(encryptedAsicData, zipOutputStream);
                log.info("asic decrypt thread dead");
            });

            return new ZipInputStream(pipedInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDecrypted(@NonNull final InputStream encryptedAsicData, @NonNull final Path targetPath) {
        try (OutputStream out1 = Files.newOutputStream(targetPath);
             ZipOutputStream out = new ZipOutputStream(out1)) {
            decrypt(encryptedAsicData, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void decrypt(@NonNull final InputStream encryptedAsic, @NonNull final ZipOutputStream zipOutputStream) {
        CMSKrypteringImpl cmsKryptering = new CMSKrypteringImpl();

        InputStream inputStream = cmsKryptering.dekrypterData(encryptedAsic, privatNokkel);
        AsicReader reader;

        try {
            reader = asicReaderFactory.open(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean entryAdded = false;
        try  {
            String filnavn;
            while ((filnavn = reader.getNextFile()) != null) {
                entryAdded = true;
                zipOutputStream.putNextEntry(new ZipEntry(filnavn));
                reader.writeFile(zipOutputStream);
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            if (!entryAdded)
                throw new RuntimeException("No entries in asic!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                closeQuietly(encryptedAsic);
                zipOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(@NonNull final AsicWriter writer, @NonNull final Payload p) {
        try {
            writer.add(p.getPayload(), p.getFilnavn());
        } catch (IOException e) {
            throw new RuntimeException("Error writing payload to asic", e);
        } finally {
            closeQuietly(p.getPayload());
        }
    }
}
