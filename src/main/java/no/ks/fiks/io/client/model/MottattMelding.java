package no.ks.fiks.io.client.model;

import lombok.*;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

@Data
@Builder
public class MottattMelding implements Melding {
    static final String NO_PAYLOAD_MESSAGE = "Meldingen har ingen payload";
    @NonNull
    private MeldingId meldingId;
    @NonNull
    private String meldingType;
    @NonNull
    private KontoId avsenderKontoId;
    @NonNull
    private KontoId mottakerKontoId;
    @NonNull
    private Duration ttl;
    @Builder.Default
    private boolean resendt = false;

    private boolean harPayload;
    private Map<String, String> headere;

    @NonNull
    @Getter(AccessLevel.NONE)
    private Consumer<Path> writeKryptertZip;
    @NonNull
    @Getter(AccessLevel.NONE)
    private Consumer<Path> writeDekryptertZip;
    @NonNull
    @Getter(AccessLevel.NONE)
    private Supplier<InputStream> getKryptertStream;
    @NonNull
    @Getter(AccessLevel.NONE)
    private Supplier<ZipInputStream> getDekryptertZipStream;

    private MeldingId svarPaMelding;

    private MeldingId klientMeldingId;

    public static MottattMelding fromMottattMeldingMetadata(
        MottattMeldingMetadata melding,
        boolean harPaylod,
        Consumer<Path> writeDekryptertZip,
        Consumer<Path> writeKryptertZip,
        Supplier<InputStream> getKryptertStream,
        Supplier<ZipInputStream> getDekryptertZipStream) {
        return MottattMelding.builder()
            .meldingId(new MeldingId(melding.getMeldingId()))
            .harPayload(harPaylod)
            .meldingType(melding.getMeldingType())
            .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
            .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
            .ttl(Duration.ofMillis(melding.getTtl()))
            .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
            .klientMeldingId(getKlientMeldingIdFromHeader(melding))
            .headere(melding.getHeadere() != null ? melding.getHeadere() : Collections.emptyMap())
            .resendt(melding.isResendt())
                .writeKryptertZip(writeKryptertZip)
                .writeDekryptertZip(writeDekryptertZip)
                .getKryptertStream(getKryptertStream)
                .getDekryptertZipStream(getDekryptertZipStream)
                .build();
    }

    public InputStream getKryptertStream() {
        if (harPayload) {
            return getKryptertStream.get();
        } else {
            throw new IllegalStateException(NO_PAYLOAD_MESSAGE);
        }

    }

    public ZipInputStream getDekryptertZipStream() {
        if (harPayload) {
            return getDekryptertZipStream.get();
        } else {
            throw new IllegalStateException(NO_PAYLOAD_MESSAGE);
        }
    }

    public void writeKryptertZip(Path path) {
        if (harPayload) {
            writeKryptertZip.accept(path);
        } else {
            throw new IllegalStateException(NO_PAYLOAD_MESSAGE);
        }
    }

    public void writeDekryptertZip(Path path) {
        if (harPayload) {
            writeDekryptertZip.accept(path);
        } else {
            throw new IllegalStateException(NO_PAYLOAD_MESSAGE);
        }
    }

    private static MeldingId getKlientMeldingIdFromHeader(MottattMeldingMetadata melding) {
        if(melding.getHeadere() != null && melding.getHeadere().get(HeaderKlientMeldingId) != null) {
            try {
                return new MeldingId(UUID.fromString(melding.getHeadere().get(HeaderKlientMeldingId)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}