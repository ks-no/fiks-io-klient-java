package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.Melding;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.svarinn2.swagger.model.v1.MottattKvittering;

import java.io.IOException;

import static lombok.AccessLevel.NONE;

@Value
@Getter(NONE)
@Builder
public class KvitteringSender {

    @NonNull private Melding meldingSomSkalKvitteres;
    @NonNull private Long deliveryTag;
    @NonNull private SvarInnApi svarInnApi;
    @NonNull private Channel channel;
    @NonNull private AsicGenerator asicGenerator;

    public Melding aksepter() {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterAkseptert(new MottattKvittering()
                .avsenderId(this.meldingSomSkalKvitteres.getMottakerKontoId().getUuid())
                .kvitteringForMeldingId(this.meldingSomSkalKvitteres.getMeldingId().getUuid()));
        ack();
        return Melding.fromSendResponse(respons);
    }

    public Melding settFeilet(String melding) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterFeilet(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                asicGenerator.encrypt(melding));
        ack();
        return Melding.fromSendResponse(respons);
    }

    public Melding avvist(String melding) {
        no.ks.fiks.svarinn2.swagger.model.v1.Melding respons = svarInnApi.kvitterFeilet(
                meldingSomSkalKvitteres.getMottakerKontoId().toString(),
                meldingSomSkalKvitteres.getAvsenderKontoId().toString(),
                meldingSomSkalKvitteres.getMeldingId().toString(),
                asicGenerator.encrypt(melding));
        ack();
        return Melding.fromSendResponse(respons);
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
