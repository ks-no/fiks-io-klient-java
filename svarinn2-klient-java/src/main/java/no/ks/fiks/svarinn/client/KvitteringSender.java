package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn.client.model.Payload;
import no.ks.fiks.svarinn.client.model.SendtMelding;
import no.ks.fiks.svarinn.client.model.StringPayload;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.svarinn2.swagger.model.v1.MottattKvittering;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class KvitteringSender {

    private static final String KVITTERING_FILNAVN = "kvitteringTekst.txt";

    @NonNull private MottattMelding meldingSomSkalKvitteres;
    @NonNull private SvarInnApi svarInnApi;
    @NonNull private Runnable doQueueAck;
    @NonNull private Function<List<Payload>, File> encrypt;

    public SendtMelding kvitterAkseptert() {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterAkseptert(new MottattKvittering()
                .avsenderId(this.meldingSomSkalKvitteres.getMottakerKontoId().getUuid())
                .kvitteringsMottakerId(this.meldingSomSkalKvitteres.getAvsenderKontoId().getUuid())
                .kvitteringForMeldingId(this.meldingSomSkalKvitteres.getMeldingId().getUuid()));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public SendtMelding kvitterFeilet() {
        return kvitterAvvist(null);
    }

    public SendtMelding kvitterFeilet(String beskjed) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterFeilet(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                beskjed == null ? null : encrypt.apply(Collections.singletonList(new StringPayload(beskjed, KVITTERING_FILNAVN))));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public SendtMelding kvitterAvvist() {
        return kvitterAvvist(null);
    }

    public SendtMelding kvitterAvvist(String beskjed) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterAvvist(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                beskjed == null ? null : encrypt.apply(Collections.singletonList(new StringPayload(beskjed, KVITTERING_FILNAVN))));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public void kvitterUtenMelding() {
        ack();
    }

    private void ack() {
        doQueueAck.run();
    }
}
