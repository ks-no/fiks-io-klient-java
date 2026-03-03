package no.ks.fiks.io.client.model;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MeldingRequestTest {

    @Test
    void testDefaultHeadere() {
        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType("meldingsprotokoll")
            .mottakerKontoId(new KontoId(UUID.randomUUID()))
            .build();

        assertNotNull(meldingRequest.getHeadere());
        assertEquals(0, meldingRequest.getHeadere().size());
    }

    @Test
    void testNullHeadere() {
        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType("meldingsprotokoll")
            .mottakerKontoId(new KontoId(UUID.randomUUID()))
            .headere(null)
            .build();

        assertNotNull(meldingRequest.getHeadere());
        assertEquals(0, meldingRequest.getHeadere().size());
    }

    @Test
    void testUtenKlientMeldingId() {
        final String meldingsprotokoll = "meldingsprotokoll";
        final UUID mottakerKontoId = UUID.randomUUID();
        final UUID svarPaMelding = UUID.randomUUID();
        final Duration ttl = Duration.ofDays(5L);

        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType(meldingsprotokoll)
            .mottakerKontoId(new KontoId(mottakerKontoId))
            .ttl(ttl)
            .svarPaMelding(new MeldingId(svarPaMelding))
            .build();

        assertEquals(meldingsprotokoll, meldingRequest.getMeldingType());
        assertEquals(mottakerKontoId, meldingRequest.getMottakerKontoId().getUuid());
        assertEquals(svarPaMelding, Objects.requireNonNull(meldingRequest.getSvarPaMelding()).getUuid());
        assertEquals(ttl, meldingRequest.getTtl());
        assertNull(meldingRequest.getKlientMeldingId());
        assertEquals(0, Objects.requireNonNull(meldingRequest.getHeadere()).size());
    }

    @Test
    void testKlientMeldingIdViaProperty() {
        final String meldingsprotokoll = "meldingsprotokoll";
        final UUID mottakerKontoId = UUID.randomUUID();
        final UUID klientMeldingId = UUID.randomUUID();
        final UUID svarPaMelding = UUID.randomUUID();
        final Duration ttl = Duration.ofDays(5L);

        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType(meldingsprotokoll)
            .mottakerKontoId(new KontoId(mottakerKontoId))
            .ttl(ttl)
            .svarPaMelding(new MeldingId(svarPaMelding))
            .klientMeldingId(new MeldingId(klientMeldingId))
            .build();

        assertEquals(meldingsprotokoll, meldingRequest.getMeldingType());
        assertEquals(mottakerKontoId, meldingRequest.getMottakerKontoId().getUuid());
        assertEquals(svarPaMelding, Objects.requireNonNull(meldingRequest.getSvarPaMelding()).getUuid());
        assertEquals(ttl, meldingRequest.getTtl());
        assertEquals(klientMeldingId, Objects.requireNonNull(meldingRequest.getKlientMeldingId()).getUuid());
        assertEquals(1, Objects.requireNonNull(meldingRequest.getHeadere()).size());
        assertEquals(klientMeldingId.toString(), meldingRequest.getHeadere().get(Melding.HeaderKlientMeldingId));
    }

    @Test
    void testKlientMeldingIdViaHeadere() {
        final String meldingsprotokoll = "meldingsprotokoll";
        final UUID mottakerKontoId = UUID.randomUUID();
        final UUID klientMeldingId = UUID.randomUUID();
        final UUID svarPaMelding = UUID.randomUUID();
        final Duration ttl = Duration.ofDays(5L);

        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType(meldingsprotokoll)
            .mottakerKontoId(new KontoId(mottakerKontoId))
            .ttl(ttl)
            .svarPaMelding(new MeldingId(svarPaMelding))
            .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId.toString()))
            .build();

        assertEquals(meldingsprotokoll, meldingRequest.getMeldingType());
        assertEquals(mottakerKontoId, meldingRequest.getMottakerKontoId().getUuid());
        assertEquals(svarPaMelding, Objects.requireNonNull(meldingRequest.getSvarPaMelding()).getUuid());
        assertEquals(ttl, meldingRequest.getTtl());
        assertNull(meldingRequest.getKlientMeldingId());
        assertEquals(1, Objects.requireNonNull(meldingRequest.getHeadere()).size());
        assertEquals(klientMeldingId.toString(), meldingRequest.getHeadere().get(Melding.HeaderKlientMeldingId));
    }

    @Test
    void testUlikKlientMeldingIdViaPropertyOgHeadere() {
        Exception exception = null;
        try {
            MeldingRequest.builder()
                .meldingType("meldingsprotokoll")
                .mottakerKontoId(new KontoId(UUID.randomUUID()))
                .ttl(Duration.ofDays(5L))
                .svarPaMelding(new MeldingId(UUID.randomUUID()))
                .klientMeldingId(new MeldingId(UUID.randomUUID()))
                .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, UUID.randomUUID().toString()))
                .build();
        } catch (IllegalArgumentException e) {
            exception = e;
        }

        assertNotNull(exception);
    }

    @Test
    void testLikKlientMeldingIdViaPropertyOgHeadere() {
        final String meldingsprotokoll = "meldingsprotokoll";
        final UUID mottakerKontoId = UUID.randomUUID();
        final UUID klientMeldingId = UUID.randomUUID();
        final UUID svarPaMelding = UUID.randomUUID();
        final Duration ttl = Duration.ofDays(5L);

        final MeldingRequest meldingRequest = MeldingRequest.builder()
            .meldingType(meldingsprotokoll)
            .mottakerKontoId(new KontoId(mottakerKontoId))
            .ttl(ttl)
            .svarPaMelding(new MeldingId(svarPaMelding))
            .klientMeldingId(new MeldingId(klientMeldingId))
            .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId.toString()))
            .build();

        assertEquals(meldingsprotokoll, meldingRequest.getMeldingType());
        assertEquals(mottakerKontoId, meldingRequest.getMottakerKontoId().getUuid());
        assertEquals(svarPaMelding, Objects.requireNonNull(meldingRequest.getSvarPaMelding()).getUuid());
        assertEquals(ttl, meldingRequest.getTtl());
        assertEquals(klientMeldingId, Objects.requireNonNull(meldingRequest.getKlientMeldingId()).getUuid());
        assertEquals(1, Objects.requireNonNull(meldingRequest.getHeadere()).size());
        assertEquals(klientMeldingId.toString(), meldingRequest.getHeadere().get(Melding.HeaderKlientMeldingId));
    }
}
