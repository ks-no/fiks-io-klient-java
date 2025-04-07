package no.ks.fiks.io.client;

import com.google.common.collect.ImmutableMap;
import no.ks.fiks.io.client.model.KlientKorrelasjonId;
import no.ks.fiks.io.client.model.Melding;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.SendtMelding;
import no.ks.fiks.io.klient.SendtMeldingApiModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SendtMeldingTest {

    @Test
    void fromSendResponse() {
        final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
            .meldingId(UUID.randomUUID())
            .mottakerKontoId(UUID.randomUUID())
            .meldingType("meldingsprotokoll")
            .avsenderKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .dokumentlagerId(UUID.randomUUID())
            .ttl(TimeUnit.DAYS.toMillis(5L))
            .headere(Collections.emptyMap())
            .build();
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);
        assertAll(
            () -> assertEquals(sendtMeldingApiModel.getMeldingId(), sendtMelding.getMeldingId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getMottakerKontoId(), sendtMelding.getMottakerKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getAvsenderKontoId(), sendtMelding.getAvsenderKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getSvarPaMelding(), sendtMelding.getSvarPaMelding().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getTtl(), sendtMelding.getTtl().toMillis())
        );
    }

    @Test
    void fromSendResponseWithKlientMeldingIdAndKorrelasjonId() {
        final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
        final KlientKorrelasjonId klientKorrelasjonsId = new KlientKorrelasjonId((UUID.randomUUID().toString()));
        assertNotNull(klientKorrelasjonsId.getKlientKorrelasjonId());

        final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
            .meldingId(UUID.randomUUID())
            .mottakerKontoId(UUID.randomUUID())
            .meldingType("meldingsprotokoll")
            .avsenderKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .dokumentlagerId(UUID.randomUUID())
            .ttl(TimeUnit.DAYS.toMillis(5L))
            .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId.toString(),
                 Melding.HeaderKlientKorrelasjonId, klientKorrelasjonsId.getKlientKorrelasjonId()
                ))
            .build();
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);
        assertAll(
            () -> assertEquals(sendtMeldingApiModel.getMeldingId(), sendtMelding.getMeldingId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getMottakerKontoId(), sendtMelding.getMottakerKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getAvsenderKontoId(), sendtMelding.getAvsenderKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getSvarPaMelding(), sendtMelding.getSvarPaMelding().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getTtl(), sendtMelding.getTtl().toMillis()),
            () -> assertEquals(sendtMeldingApiModel.getHeadere().get(Melding.HeaderKlientMeldingId), sendtMelding.getKlientMeldingId().toString()),
            () -> assertEquals(sendtMeldingApiModel.getHeadere().get(Melding.HeaderKlientKorrelasjonId), sendtMelding.getKlientKorrelasjonId().getKlientKorrelasjonId())
        );
    }

    @Test
    void fromSendResponseWithKlientMeldingIdThatIsNotValidUUID() {
        final String klientMeldingId = "NotUUID";
        final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
            .meldingId(UUID.randomUUID())
            .mottakerKontoId(UUID.randomUUID())
            .meldingType("meldingsprotokoll")
            .avsenderKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .dokumentlagerId(UUID.randomUUID())
            .ttl(TimeUnit.DAYS.toMillis(5L))
            .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId))
            .build();
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);
        assertAll(
            () -> assertEquals(sendtMeldingApiModel.getMeldingId(), sendtMelding.getMeldingId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getMottakerKontoId(), sendtMelding.getMottakerKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getAvsenderKontoId(), sendtMelding.getAvsenderKontoId().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getSvarPaMelding(), sendtMelding.getSvarPaMelding().getUuid()),
            () -> assertEquals(sendtMeldingApiModel.getTtl(), sendtMelding.getTtl().toMillis()),
            () -> assertEquals(sendtMeldingApiModel.getHeadere().get(Melding.HeaderKlientMeldingId), sendtMelding.getHeadere().get(Melding.HeaderKlientMeldingId)),
            () -> assertNull(sendtMelding.getKlientMeldingId())
        );
    }
}