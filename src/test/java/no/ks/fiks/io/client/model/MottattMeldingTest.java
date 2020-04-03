package no.ks.fiks.io.client.model;

import com.google.common.collect.ImmutableMap;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            false, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertEquals(mottattMeldingMetadata.getMeldingId(), mottattMelding.getMeldingId().getUuid());
        assertEquals(mottattMeldingMetadata.getMeldingType(), mottattMelding.getMeldingType());
        assertEquals(mottattMeldingMetadata.getAvsenderKontoId(), mottattMelding.getAvsenderKontoId().getUuid());
        assertEquals(mottattMeldingMetadata.getMottakerKontoId(), mottattMelding.getMottakerKontoId().getUuid());
        assertEquals(mottattMeldingMetadata.getSvarPaMelding(), mottattMelding.getSvarPaMelding().getUuid());
        assertEquals(Duration.ofMillis(mottattMeldingMetadata.getTtl()), mottattMelding.getTtl());
        assertEquals(mottattMeldingMetadata.isResendt(), mottattMelding.isResendt());
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
            false, path -> {
            },
            path -> {
            },
            () -> new NullInputStream(0L), () -> new ZipInputStream(new NullInputStream(100L)));
        assertEquals(mottattMeldingMetadata.getMeldingId(), mottattMelding.getMeldingId().getUuid());
        assertEquals(mottattMeldingMetadata.getHeadere(), mottattMelding.getHeadere());
    }
}