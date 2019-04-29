package no.ks.fiks.io.client.model;

import lombok.*;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;
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

    @NonNull @Getter(AccessLevel.NONE) private Consumer<Path> writeKryptertZip;
    @NonNull @Getter(AccessLevel.NONE) private Consumer<Path> writeDekryptertZip;
    @NonNull @Getter(AccessLevel.NONE) private Supplier<InputStream> getKryptertStream;
    @NonNull @Getter(AccessLevel.NONE) private Supplier<ZipInputStream> getDekryptertZipStream;

    private MeldingId svarPaMelding;

    public static MottattMelding fromMottattMeldingMetadata(
            MottattMeldingMetadata melding,
            Consumer<Path> writeDekryptertZip,
            Consumer<Path> writeKryptertZip,
            Supplier<InputStream> getKryptertStream,
            Supplier<ZipInputStream> getDekryptertZipStream) {
        return MottattMelding.builder()
                .meldingId(new MeldingId(melding.getMeldingId()))
                .meldingType(melding.getMeldingType())
                .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
                .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
                .ttl(Duration.ofMillis(melding.getTtl()))
                .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
                .writeKryptertZip(writeKryptertZip)
                .writeDekryptertZip(writeDekryptertZip)
                .getKryptertStream(getKryptertStream)
                .getDekryptertZipStream(getDekryptertZipStream)
                .build();
    }

    public InputStream getKryptertStream(){
        return getKryptertStream.get();
    }

    public ZipInputStream getDekryptertZipStream(){
        return getDekryptertZipStream.get();
    }

    public void writeKryptertZip(Path path){
        writeKryptertZip.accept(path);
    }

    public void writeDekryptertZip(Path path){
        writeDekryptertZip.accept(path);
    }

}