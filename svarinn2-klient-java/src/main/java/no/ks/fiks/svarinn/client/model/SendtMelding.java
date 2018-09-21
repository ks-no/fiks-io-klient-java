package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;

@Data
@Builder
public class SendtMelding implements Melding{
    @NonNull private MeldingId meldingId;
    @NonNull private String meldingType;
    @NonNull private KontoId avsenderKontoId;
    @NonNull private KontoId mottakerKontoId;
    @NonNull private Duration ttl;
    private MeldingId svarPaMelding;

    public static SendtMelding fromSendResponse(no.ks.fiks.svarinn2.swagger.model.v1.Melding melding) {
        return SendtMelding.builder()
                .meldingId(new MeldingId(melding.getMeldingId()))
                .meldingType(melding.getMeldingType())
                .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
                .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
                .ttl(Duration.ofMillis(melding.getTtl()))
                .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
                .build();
    }
}