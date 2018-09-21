package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn.client.model.SendtMelding;
import no.ks.fiks.svarinn.client.model.StringPayload;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.svarinn2.swagger.model.v1.MottattKvittering;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class KvitteringSender {

    private static final String KVITTERING_FILNAVN = "kvitteringTekst.txt";
    @NonNull private MottattMelding meldingSomSkalKvitteres;
    @NonNull private X509Certificate mottakerSertifikat;
    @NonNull private Long deliveryTag;
    @NonNull private SvarInnApi svarInnApi;
    @NonNull private Channel channel;
    @NonNull private AsicHandler asicGenerator;

    public SendtMelding kvitterAkseptert() {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterAkseptert(new MottattKvittering()
                .avsenderId(this.meldingSomSkalKvitteres.getMottakerKontoId().getUuid())
                .kvitteringsMottakerId(this.meldingSomSkalKvitteres.getAvsenderKontoId().getUuid())
                .kvitteringForMeldingId(this.meldingSomSkalKvitteres.getMeldingId().getUuid()));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public SendtMelding kvitterFeilet(String melding) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterFeilet(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                asicGenerator.encrypt(mottakerSertifikat, Collections.singletonList(new StringPayload(melding, KVITTERING_FILNAVN) )));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public SendtMelding kvitterAvvist(String melding) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterAvvist(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                asicGenerator.encrypt(mottakerSertifikat, Collections.singletonList(new StringPayload(melding, KVITTERING_FILNAVN) )));
        ack();
        return SendtMelding.fromSendResponse(respons);
    }

    public void kvitterUtenMelding() {
        ack();
    }

    private void ack(){
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            throw new RuntimeException("Feil under acking mot rabbitmq", e);
        }
    }
}
