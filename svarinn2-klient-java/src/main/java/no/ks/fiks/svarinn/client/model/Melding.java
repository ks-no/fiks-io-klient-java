package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.KontoId;
import no.ks.fiks.svarinn.client.MeldingId;
import no.ks.fiks.svarinn2.model.MottattMelding;

import java.time.Duration;

@Data
@Builder
public class Melding {
    @NonNull private MeldingId meldingId;
    @NonNull private String meldingType;
    @NonNull private KontoId avsenderKontoId;
    @NonNull private KontoId mottakerKontoId;
    @NonNull private Duration ttl;
    private MeldingId svarPaMelding;


    public static Melding fromSendResponse(no.ks.fiks.svarinn2.swagger.model.v1.Melding melding) {
        return Melding.builder()
                .meldingId(new MeldingId(melding.getMeldingId()))
                .meldingType(melding.getMeldingType())
                .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
                .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
                .ttl(Duration.ofMillis(melding.getTtl()))
                .svarPaMelding(new MeldingId(melding.getSvarPaMelding()))
                .build();
    }

    public static Melding fromMottattMelding(MottattMelding melding) {
        return Melding.builder()
                .meldingId(new MeldingId(melding.getMeldingId()))
                .meldingType(melding.getMeldingType())
                .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
                .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
                .ttl(Duration.ofMillis(melding.getTtl()))
                .svarPaMelding(new MeldingId(melding.getSvarPaMelding()))
                .build();
    }
}