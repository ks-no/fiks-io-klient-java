package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Value
@Builder
public class MeldingRequest implements MeldingSpesifikasjon {
    @NonNull private KontoId mottakerKontoId;
    @NonNull private String meldingType;
    @Builder.Default private Duration ttl = Duration.ofDays(2);
    private MeldingId svarPaMelding;
    private MeldingId klientMeldingId;
    @Builder.Default private Map<String, String> headere = Collections.emptyMap();
}
