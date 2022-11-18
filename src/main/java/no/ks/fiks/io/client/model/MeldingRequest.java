package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Value
@Builder
public class MeldingRequest implements MeldingSpesifikasjon {
    @NonNull private KontoId mottakerKontoId;
    @NonNull private String meldingType;
    @Nullable private Duration ttl;
    @Nullable private MeldingId svarPaMelding;
    @Nullable private MeldingId klientMeldingId;
    @Builder.Default private Map<String, String> headere = Collections.emptyMap();
}
