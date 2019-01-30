package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class MeldingRequest implements MeldingSpesifikasjon {
    @NonNull private KontoId mottakerKontoId;
    @NonNull private String meldingType;
    @Builder.Default private Duration ttl = Duration.ofDays(2);
    private MeldingId svarPaMelding;
}
