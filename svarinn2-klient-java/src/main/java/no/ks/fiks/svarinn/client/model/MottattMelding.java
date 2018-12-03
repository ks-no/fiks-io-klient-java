package no.ks.fiks.svarinn.client.model;

import lombok.*;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

@Data
@Builder
public class MottattMelding implements Melding {
    @NonNull private MeldingId meldingId;
    @NonNull private String meldingType;
    @NonNull private KontoId avsenderKontoId;
    @NonNull private KontoId mottakerKontoId;
    @NonNull private Duration ttl;
    @NonNull @Getter(AccessLevel.NONE) private Supplier<InputStream> getKryptertPayload;
    @NonNull @Getter(AccessLevel.NONE) private Supplier<ZipInputStream> getDekryptertPayload;

    private MeldingId svarPaMelding;

    public static MottattMelding fromMottattMeldingMetadata(MottattMeldingMetadata melding, Supplier<InputStream> getKryptertPayload, Supplier<ZipInputStream> getDekryptertPayload) {
        return MottattMelding.builder()
                .meldingId(new MeldingId(melding.getMeldingId()))
                .meldingType(melding.getMeldingType())
                .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
                .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
                .ttl(Duration.ofMillis(melding.getTtl()))
                .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
                .getKryptertPayload(getKryptertPayload)
                .getDekryptertPayload(getDekryptertPayload)
                .build();
    }

    public InputStream getKryptertPayload(){
        return getKryptertPayload.get();
    }

    public ZipInputStream getDekryptertPayload(){
        return getDekryptertPayload.get();
    }
}