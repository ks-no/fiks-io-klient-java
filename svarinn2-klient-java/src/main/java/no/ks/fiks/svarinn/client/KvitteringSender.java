package no.ks.fiks.svarinn.client;

import io.vavr.control.Option;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class KvitteringSender {

    @NonNull private MottattMelding meldingSomSkalKvitteres;
    @NonNull private SvarInnUtsendingKlient utsendingKlient;
    @NonNull private Runnable doQueueAck;
    @NonNull private Function<List<Payload>, InputStream> encrypt;

    public SendtMelding svar(String meldingType, List<Payload> payloads) {
        return SendtMelding.fromSendResponse(utsendingKlient.send(
                MeldingSpesifikasjonApiModel.builder()
                        .avsenderKontoId(meldingSomSkalKvitteres.getMottakerKontoId().getUuid())
                        .mottakerKontoId(meldingSomSkalKvitteres.getAvsenderKontoId().getUuid())
                        .svarPaMelding(meldingSomSkalKvitteres.getMeldingId().getUuid())
                        .meldingType(meldingType)
                        .build(), payloads.isEmpty() ? Option.none() : Option.of(encrypt.apply(payloads))));
    }

    public SendtMelding svar(String meldingType, InputStream melding, String filnavn) {
        return svar(meldingType, singletonList(new StreamPayload(melding, filnavn)));
    }

    public SendtMelding svar(String meldingType, String melding, String filnavn) {
        return svar(meldingType, singletonList(new StringPayload(melding, filnavn)));
    }

    public SendtMelding svar(String meldingType, File melding) {
        return svar(meldingType, singletonList(new FilePayload(melding)));
    }

    public SendtMelding svar(String meldingType) {
        return svar(meldingType, Collections.emptyList());
    }

    public void ack() {
        doQueueAck.run();
    }
}
