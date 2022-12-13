package no.ks.fiks.io.client.model;

import com.google.common.collect.ImmutableMap;
import net.bytebuddy.utility.RandomString;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class MottattMeldingTest {

    @DisplayName("Bygger mottatt melding for melding som er resendt")
    @Test
    void fromMottattMeldingMetadataResendt() {
        final MottattMeldingMetadata mottattMeldingMetadata = MottattMeldingMetadata.builder()
            .avsenderKontoId(UUID.randomUUID())
            .meldingId(UUID.randomUUID())
            .meldingType("meldingType")
            .mottakerKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .deliveryTag(Long.MAX_VALUE)
            .ttl(Duration.ofMinutes(22L).toMillis())
            .resendt(true)
            .build();
        final MottattMelding mottattMelding = MottattMelding.fromMottattMeldingMetadata(
            mottattMeldingMetadata,
            true, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertTrue(mottattMelding.isHarPayload());
        assertEquals(mottattMeldingMetadata.getMeldingId(), mottattMelding.getMeldingId().getUuid());
        assertEquals(mottattMeldingMetadata.getMeldingType(), mottattMelding.getMeldingType());
        assertEquals(mottattMeldingMetadata.getAvsenderKontoId(), mottattMelding.getAvsenderKontoId().getUuid());
        assertEquals(mottattMeldingMetadata.getMottakerKontoId(), mottattMelding.getMottakerKontoId().getUuid());
        assertEquals(mottattMeldingMetadata.getSvarPaMelding(), mottattMelding.getSvarPaMelding().getUuid());
        assertEquals(Duration.ofMillis(mottattMeldingMetadata.getTtl()), mottattMelding.getTtl());
        assertEquals(mottattMeldingMetadata.isResendt(), mottattMelding.isResendt());
    }

    @DisplayName("Bygger melding uten payload")
    @Test
    void fraMeldingUtenPayload() throws IOException {
        final MottattMeldingMetadata mottattMeldingMetadata = MottattMeldingMetadata.builder()
            .avsenderKontoId(UUID.randomUUID())
            .meldingId(UUID.randomUUID())
            .meldingType("meldingType")
            .mottakerKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .deliveryTag(Long.MAX_VALUE)
            .ttl(Duration.ofMinutes(22L).toMillis())
            .resendt(true)
            .build();
        final MottattMelding mottattMelding = MottattMelding.fromMottattMeldingMetadata(
            mottattMeldingMetadata,
            false, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertFalse(mottattMelding.isHarPayload());

        final Path tempDirectory = Files.createTempDirectory(RandomString.make(10));
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.getDekryptertZipStream()).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.getKryptertStream()).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.writeDekryptertZip(tempDirectory)).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.writeKryptertZip(tempDirectory)).getMessage());
        Files.delete(tempDirectory);
    }

    @DisplayName("Bygger melding uten ttl")
    @Test
    void meldingUtenTTL() throws IOException {
        final MottattMeldingMetadata mottattMeldingMetadata = MottattMeldingMetadata.builder()
            .avsenderKontoId(UUID.randomUUID())
            .meldingId(UUID.randomUUID())
            .meldingType("meldingType")
            .mottakerKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .deliveryTag(Long.MAX_VALUE)
            .resendt(true)
            .build();
        final MottattMelding mottattMelding = MottattMelding.fromMottattMeldingMetadata(
            mottattMeldingMetadata,
            false, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertFalse(mottattMelding.isHarPayload());

        final Path tempDirectory = Files.createTempDirectory(RandomString.make(10));
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.getDekryptertZipStream()).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.getKryptertStream()).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.writeDekryptertZip(tempDirectory)).getMessage());
        assertEquals(MottattMelding.NO_PAYLOAD_MESSAGE, assertThrows(IllegalStateException.class, () -> mottattMelding.writeKryptertZip(tempDirectory)).getMessage());
        Files.delete(tempDirectory);
    }

    @DisplayName("Bygger mottatt melding inkludert headers")
    @Test
    void fromMottattMeldingMetadataMedHeaders() {
        final MottattMeldingMetadata mottattMeldingMetadata = MottattMeldingMetadata.builder()
            .avsenderKontoId(UUID.randomUUID())
            .meldingId(UUID.randomUUID())
            .meldingType("meldingType")
            .mottakerKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .deliveryTag(Long.MAX_VALUE)
            .ttl(Duration.ofMinutes(22L).toMillis())
            .headere(ImmutableMap.of("header1", "verdi1", "header2", "verdi2"))
            .build();
        final MottattMelding mottattMelding = MottattMelding.fromMottattMeldingMetadata(
            mottattMeldingMetadata,
            true, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertEquals(mottattMeldingMetadata.getMeldingId(), mottattMelding.getMeldingId().getUuid());
        assertEquals(mottattMeldingMetadata.getHeadere(), mottattMelding.getHeadere());
    }

    @DisplayName("Bygger mottatt melding inkludert klientMeldingId")
    @Test
    void fromMottattMeldingMetadataMedKlientMeldingId() {
        final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
        final MottattMeldingMetadata mottattMeldingMetadata = MottattMeldingMetadata.builder()
            .avsenderKontoId(UUID.randomUUID())
            .meldingId(UUID.randomUUID())
            .meldingType("meldingType")
            .mottakerKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .deliveryTag(Long.MAX_VALUE)
            .ttl(Duration.ofMinutes(22L).toMillis())
            .headere(ImmutableMap.of("header1", "verdi1", "header2", "verdi2", Melding.HeaderKlientMeldingId, klientMeldingId.toString()))
            .build();
        final MottattMelding mottattMelding = MottattMelding.fromMottattMeldingMetadata(
            mottattMeldingMetadata,
            true, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertEquals(klientMeldingId, mottattMelding.getKlientMeldingId());
    }
}