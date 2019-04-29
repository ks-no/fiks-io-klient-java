package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ks.fiks.svarinn2.klient.SendtMeldingApiModel;

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

    public static SendtMelding fromSendResponse(@NonNull SendtMeldingApiModel melding) {
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