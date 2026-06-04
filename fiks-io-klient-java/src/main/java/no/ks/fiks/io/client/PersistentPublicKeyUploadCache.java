package no.ks.fiks.io.client;

import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.client.model.KontoId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PersistentPublicKeyUploadCache implements PublicKeyUploadRateLimiter {

    private final Duration uploadInterval;
    private final Path cacheFile;
    private Map<String, Instant> uploadTimes = new HashMap<>();

    public PersistentPublicKeyUploadCache(Duration uploadInterval, Path cacheFile) {
        this.uploadInterval = uploadInterval;
        this.cacheFile = cacheFile;
        loadFromDisk();
    }

    public PersistentPublicKeyUploadCache(long uploadIntervalHours) {
        this(Duration.ofHours(uploadIntervalHours), Paths.get(System.getProperty("user.home"), ".fiks-io", "public-key-upload-cache.txt"));
    }

    public PersistentPublicKeyUploadCache(Duration uploadInterval) {
        this(uploadInterval, Paths.get(System.getProperty("user.home"), ".fiks-io", "public-key-upload-cache.txt"));
    }

    public boolean shouldUpload(KontoId kontoId) {
        Instant lastUpload = uploadTimes.get(kontoId.getUuid().toString());

        if (lastUpload == null) {
            log.debug("Ingen tidligere opplasting av public key for konto {}, skal opplaste", kontoId);
            return true;
        }

        Instant nextAllowedUpload = lastUpload.plus(uploadInterval);
        if (Instant.now().isBefore(nextAllowedUpload)) {
            log.info("Public key for konto {} ble opplastet for {} siden, venter {}",
                kontoId, Duration.between(lastUpload, Instant.now()), uploadInterval);
            return false;
        }

        log.debug("Tilstrekkelig tid har gått siden sist opplasting av public key for konto {}", kontoId);
        return true;
    }

    public Instant nextUploadAllowedAfter(KontoId kontoId){
        Instant lastUpload = uploadTimes.get(kontoId.getUuid().toString());

        if (lastUpload == null) {
            return Instant.now();
        }

        return lastUpload.plus(uploadInterval);
    }

    public void recordUpload(KontoId kontoId) {
        Instant now = Instant.now();
        uploadTimes.put(kontoId.getUuid().toString(), now);
        saveToDisk();
        log.debug("Registrerte opplasting av public key for konto {} på {}", kontoId, now);
    }

    private void loadFromDisk() {
        try {
            if (!Files.exists(cacheFile)) {
                return;
            }

            Files.lines(cacheFile).forEach(line -> {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    try {
                        String kontoId = parts[0];
                        Instant instant = Instant.parse(parts[1]);
                        uploadTimes.put(kontoId, instant);
                    } catch (Exception e) {
                        log.warn("Kunne ikke parse cache-linje: {}", line);
                    }
                }
            });
            log.debug("Lastet {} public key upload-tider fra cache-fil", uploadTimes.size());
        } catch (IOException e) {
            log.warn("Kunne ikke laste public key upload-cache fra disk", e);
        }
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(cacheFile.getParent());
            StringBuilder sb = new StringBuilder();
            uploadTimes.forEach((kontoId, instant) ->
                sb.append(kontoId).append("=").append(instant).append("\n")
            );
            Files.writeString(cacheFile, sb.toString());
            log.debug("Lagret public key upload-cache til disk");
        } catch (IOException e) {
            log.warn("Kunne ikke lagre public key upload-cache til disk", e);
        }
    }
}
