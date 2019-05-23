package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class LookupRequest {
    @NonNull private Identifikator identifikator;
    @NonNull private String meldingsprotokoll;
    @NonNull private Integer sikkerhetsNiva;
}
