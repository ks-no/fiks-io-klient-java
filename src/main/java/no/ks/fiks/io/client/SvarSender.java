package no.ks.fiks.io.client;

import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.io.client.model.*;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class SvarSender {

    @NonNull private MottattMelding meldingSomSkalKvitteres;
    @NonNull private FiksIOSender utsendingKlient;
    @NonNull
    private AmqpChannelFeedbackHandler amqpChannelFeedbackHandler;
    @NonNull private Function<List<Payload>, InputStream> encrypt;

    public SendtMelding svar(String meldingType, List<Payload> payloads) {
        return SendtMelding.fromSendResponse(utsendingKlient.send(
            fellesBuilder(meldingType).build(), Optional.ofNullable(payloads).filter(p -> ! p.isEmpty()).map(encrypt)));
    }

    public SendtMelding svar(String meldingType, List<Payload> payloads, MeldingId klientMeldingId) {
        return SendtMelding.fromSendResponse(utsendingKlient.send(
            fellesBuilder(meldingType)
            .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, klientMeldingId.toString()))
            .build(), Optional.ofNullable(payloads).filter(p -> ! p.isEmpty()).map(encrypt)));
    }

    public SendtMelding svar(String meldingType, InputStream melding, String filnavn) {
        return svar(meldingType, singletonList(new StreamPayload(melding, filnavn)));
    }

    public SendtMelding svar(String meldingType, InputStream melding, String filnavn, MeldingId klientMeldingId) {
        return svar(meldingType, singletonList(new StreamPayload(melding, filnavn)), klientMeldingId);
    }

    public SendtMelding svar(String meldingType, String melding, String filnavn) {
        return svar(meldingType, singletonList(new StringPayload(melding, filnavn)));
    }

    public SendtMelding svar(String meldingType, String melding, String filnavn, MeldingId klientMeldingId) {
        return svar(meldingType, singletonList(new StringPayload(melding, filnavn)), klientMeldingId);
    }

    public SendtMelding svar(String meldingType, Path melding) {
        return svar(meldingType, singletonList(new FilePayload(melding)));
    }

    public SendtMelding svar(String meldingType, Path melding, MeldingId klientMeldingId) {
        return svar(meldingType, singletonList(new FilePayload(melding)), klientMeldingId);
    }

    public SendtMelding svar(String meldingType) {
        return svar(meldingType, Collections.emptyList());
    }

    public SendtMelding svar(String meldingType, MeldingId klientMeldingId) {
        return svar(meldingType, Collections.emptyList(), klientMeldingId);
    }

    public void ack() {
        amqpChannelFeedbackHandler.getHandleAck().run();
    }

    public void nack() {
        amqpChannelFeedbackHandler.getHandleNack().run();
    }

    public void nackWithRequeue() {
        amqpChannelFeedbackHandler.getHandleNackWithRequeue().run();
    }

    private MeldingSpesifikasjonApiModel.MeldingSpesifikasjonApiModelBuilder fellesBuilder(String meldingType) {
        return MeldingSpesifikasjonApiModel.builder()
            .avsenderKontoId(meldingSomSkalKvitteres.getMottakerKontoId().getUuid())
            .mottakerKontoId(meldingSomSkalKvitteres.getAvsenderKontoId().getUuid())
            .svarPaMelding(meldingSomSkalKvitteres.getMeldingId().getUuid())
            .meldingType(meldingType);
    }
}
