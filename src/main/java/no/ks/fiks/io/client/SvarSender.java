package no.ks.fiks.io.client;

import io.vavr.control.Option;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.io.client.model.FilePayload;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.client.model.Payload;
import no.ks.fiks.io.client.model.SendtMelding;
import no.ks.fiks.io.client.model.StreamPayload;
import no.ks.fiks.io.client.model.StringPayload;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.svarinn2.klient.MeldingSpesifikasjonApiModel;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class SvarSender {

    @NonNull private MottattMelding meldingSomSkalKvitteres;
    @NonNull private FiksIOSender utsendingKlient;
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

    public SendtMelding svar(String meldingType, Path melding) {
        return svar(meldingType, singletonList(new FilePayload(melding)));
    }

    public SendtMelding svar(String meldingType) {
        return svar(meldingType, Collections.emptyList());
    }

    public void ack() {
        doQueueAck.run();
    }
}
