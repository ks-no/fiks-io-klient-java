package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SendtMelding implements Melding {
    @NonNull private MeldingId meldingId;
    @NonNull private KontoId avsenderKontoId;
    @NonNull private String meldingType;
    @NonNull private KontoId mottakerKontoId;
    @NonNull private Duration ttl;
    private Map<String, String> headere;
    private MeldingId svarPaMelding;
    private MeldingId klientMeldingId;

    public static SendtMelding fromSendResponse(@NonNull SendtMeldingApiModel melding) {
        return SendtMelding.builder()
            .meldingId(new MeldingId(melding.getMeldingId()))
            .avsenderKontoId(new KontoId(melding.getAvsenderKontoId()))
            .meldingType(melding.getMeldingType())
            .mottakerKontoId(new KontoId(melding.getMottakerKontoId()))
            .ttl(Duration.ofMillis(melding.getTtl()))
            .headere(melding.getHeadere() != null ? melding.getHeadere() : Collections.emptyMap())
            .svarPaMelding(melding.getSvarPaMelding() != null ? new MeldingId(melding.getSvarPaMelding()) : null)
            .klientMeldingId(getKlientMeldingIdFromHeader(melding))
            .build();
    }

    private static MeldingId getKlientMeldingIdFromHeader(SendtMeldingApiModel melding) {
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