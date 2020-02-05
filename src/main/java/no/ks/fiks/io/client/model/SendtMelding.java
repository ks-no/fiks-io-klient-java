package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class SendtMelding implements Melding {
    @NonNull private MeldingId meldingId;
    @NonNull private KontoId avsenderKontoId;
    @NonNull private String meldingType;
    @NonNull private KontoId mottakerKontoId;
    @NonNull private Duration ttl;
    @NonNull private Map<String, String> headere;
    private MeldingId svarPaMelding;

    public static SendtMelding fromSendResponse(@NonNull SendtMeldingApiModel melding) {
        return SendtMelding.builder()
            .meldingId(new MeldingId(melding.getMeldingId()))
            .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
            .meldingType(melding.getMeldingType())
            .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
            .ttl(Duration.ofMillis(melding.getTtl()))
            .headere(melding.getHeadere())
            .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
            .build();
    }
}