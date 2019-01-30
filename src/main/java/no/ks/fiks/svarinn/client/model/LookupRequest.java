package no.ks.fiks.svarinn.client.model;

import io.swagger.client.model.Identifikator;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class LookupRequest {
    @NonNull private Identifikator identifikator;

    @NonNull
    private String meldingType;
    @NonNull private Integer sikkerhetsNiva;
}
