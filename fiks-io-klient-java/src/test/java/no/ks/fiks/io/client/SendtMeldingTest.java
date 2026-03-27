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
    void fromSendResponseWithKlientMeldingIdAndKorrelasjonIdAndKorrelasjonIdDeprecated() {
        final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
        final KlientKorrelasjonId klientKorrelasjonsId = new KlientKorrelasjonId((UUID.randomUUID().toString()));
        assertNotNull(klientKorrelasjonsId.getKlientKorrelasjonId());

        final var headere = ImmutableMap.of(
            Melding.HeaderKlientMeldingId, klientMeldingId.toString(),
            Melding.HeaderKlientKorrelasjonIdDeprecated, UUID.randomUUID().toString(),
            Melding.HeaderKlientKorrelasjonId, klientKorrelasjonsId.getKlientKorrelasjonId()
        );

        final SendtMeldingApiModel sendtMeldingApiModel = lagSendtMeldingApiModel(headere);
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);

        validerSendtMelding(sendtMeldingApiModel, sendtMelding, klientKorrelasjonsId.getKlientKorrelasjonId());
    }

    @Test
    void fromSendResponseWithKlientMeldingIdAndKorrelasjonId() {
        final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
        final KlientKorrelasjonId klientKorrelasjonsId = new KlientKorrelasjonId((UUID.randomUUID().toString()));
        assertNotNull(klientKorrelasjonsId.getKlientKorrelasjonId());

        final var headere = ImmutableMap.of(
            Melding.HeaderKlientMeldingId, klientMeldingId.toString(),
            Melding.HeaderKlientKorrelasjonId, klientKorrelasjonsId.getKlientKorrelasjonId()
        );

        final SendtMeldingApiModel sendtMeldingApiModel = lagSendtMeldingApiModel(headere);
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);

        validerSendtMelding(sendtMeldingApiModel, sendtMelding, klientKorrelasjonsId.getKlientKorrelasjonId());
    }

    @Test
    void fromSendResponseWithKlientMeldingIdAndKorrelasjonIdDeprecated() {
        final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
        final KlientKorrelasjonId klientKorrelasjonsId = new KlientKorrelasjonId((UUID.randomUUID().toString()));
        assertNotNull(klientKorrelasjonsId.getKlientKorrelasjonId());

        final var headere = ImmutableMap.of(
            Melding.HeaderKlientMeldingId, klientMeldingId.toString(),
            Melding.HeaderKlientKorrelasjonIdDeprecated, klientKorrelasjonsId.getKlientKorrelasjonId()
        );

        final SendtMeldingApiModel sendtMeldingApiModel = lagSendtMeldingApiModel(headere);
        final SendtMelding sendtMelding = SendtMelding.fromSendResponse(sendtMeldingApiModel);

        validerSendtMelding(sendtMeldingApiModel, sendtMelding, klientKorrelasjonsId.getKlientKorrelasjonId());
    }

    private static void validerSendtMelding(SendtMeldingApiModel sendtMeldingApiModel, SendtMelding sendtMelding, String klientKorrelasjonId) {
        assertAll(
            () -> assertEquals(sendtMelding.getMeldingId().getUuid(), sendtMeldingApiModel.getMeldingId()),
            () -> assertEquals(sendtMelding.getMottakerKontoId().getUuid(), sendtMeldingApiModel.getMottakerKontoId()),
            () -> assertEquals(sendtMelding.getAvsenderKontoId().getUuid(), sendtMeldingApiModel.getAvsenderKontoId()),
            () -> assertEquals(sendtMelding.getSvarPaMelding().getUuid(), sendtMeldingApiModel.getSvarPaMelding()),
            () -> assertEquals(sendtMelding.getTtl().toMillis(), sendtMeldingApiModel.getTtl()),
            () -> assertEquals(sendtMelding.getKlientMeldingId().toString(), sendtMeldingApiModel.getHeadere().get(Melding.HeaderKlientMeldingId)),
            () -> assertEquals(sendtMelding.getKlientKorrelasjonId().toString(), klientKorrelasjonId)
        );
    }

    @Test
    void fromSendResponseWithKlientMeldingIdThatIsNotValidUUID() {
        final String klientMeldingId = "NotUUID";
        final SendtMeldingApiModel sendtMeldingApiModel = lagSendtMeldingApiModel(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId));
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

    private static SendtMeldingApiModel lagSendtMeldingApiModel(ImmutableMap<String, String> headere) {
        return SendtMeldingApiModel.builder()
            .meldingId(UUID.randomUUID())
            .mottakerKontoId(UUID.randomUUID())
            .meldingType("meldingsprotokoll")
            .avsenderKontoId(UUID.randomUUID())
            .svarPaMelding(UUID.randomUUID())
            .dokumentlagerId(UUID.randomUUID())
            .ttl(TimeUnit.DAYS.toMillis(5L))
            .headere(
                headere
            )
            .build();
    }
}