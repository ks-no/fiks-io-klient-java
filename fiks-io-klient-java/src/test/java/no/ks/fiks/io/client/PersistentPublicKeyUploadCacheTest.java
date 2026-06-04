package no.ks.fiks.io.client;

import no.ks.fiks.io.client.model.KontoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Persistent public key upload cache")
class PersistentPublicKeyUploadCacheTest {

    @DisplayName("returnerer true første gang (ingen tidligere opplasting)")
    @Test
    void shouldUploadReturnsTrueWhenNeverUploaded(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("cache.txt");
        PersistentPublicKeyUploadCache cache = new PersistentPublicKeyUploadCache(Duration.ofHours(1), cacheFile);
        KontoId kontoId = new KontoId(UUID.randomUUID());

        assertTrue(cache.shouldUpload(kontoId));
    }

    @DisplayName("returnerer false innen intervallet")
    @Test
    void shouldUploadReturnsFalseWithinInterval(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("cache.txt");
        PersistentPublicKeyUploadCache cache = new PersistentPublicKeyUploadCache(Duration.ofHours(24), cacheFile);
        KontoId kontoId = new KontoId(UUID.randomUUID());

        cache.recordUpload(kontoId);
        assertFalse(cache.shouldUpload(kontoId));
    }

    @DisplayName("returnerer true etter intervallet (med mock tid)")
    @Test
    void shouldUploadReturnsTrueAfterInterval(@TempDir Path tempDir) throws InterruptedException {
        Path cacheFile = tempDir.resolve("cache.txt");
        PersistentPublicKeyUploadCache cache = new PersistentPublicKeyUploadCache(Duration.ofMillis(100), cacheFile);
        KontoId kontoId = new KontoId(UUID.randomUUID());

        cache.recordUpload(kontoId);
        assertFalse(cache.shouldUpload(kontoId));

        Thread.sleep(150);
        assertTrue(cache.shouldUpload(kontoId));
    }

    @DisplayName("gjenoppretter cache fra disk ved oppstart")
    @Test
    void restoresCacheFromDiskOnStartup(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("cache.txt");
        KontoId kontoId = new KontoId(UUID.randomUUID());

        // Opprett cache og lagre opplasting
        PersistentPublicKeyUploadCache cache1 = new PersistentPublicKeyUploadCache(Duration.ofHours(24), cacheFile);
        cache1.recordUpload(kontoId);
        assertFalse(cache1.shouldUpload(kontoId));

        // Opprett ny cache-instans som skal laste fra disk
        PersistentPublicKeyUploadCache cache2 = new PersistentPublicKeyUploadCache(Duration.ofHours(24), cacheFile);
        assertFalse(cache2.shouldUpload(kontoId), "Cache skal være gjenopprettet fra disk");
    }

    @DisplayName("håndterer flere kontoer uavhengig")
    @Test
    void handleMultipleAccountsIndependently(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("cache.txt");
        PersistentPublicKeyUploadCache cache = new PersistentPublicKeyUploadCache(Duration.ofHours(24), cacheFile);
        KontoId kontoId1 = new KontoId(UUID.randomUUID());
        KontoId kontoId2 = new KontoId(UUID.randomUUID());

        cache.recordUpload(kontoId1);
        assertFalse(cache.shouldUpload(kontoId1));
        assertTrue(cache.shouldUpload(kontoId2));
    }

    @DisplayName("cache-fil finnes etter opplasting")
    @Test
    void cacheFileIsCreated(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("subdir").resolve("cache.txt");
        PersistentPublicKeyUploadCache cache = new PersistentPublicKeyUploadCache(Duration.ofHours(24), cacheFile);
        KontoId kontoId = new KontoId(UUID.randomUUID());

        assertFalse(Files.exists(cacheFile));
        cache.recordUpload(kontoId);
        assertTrue(Files.exists(cacheFile));
    }
}
