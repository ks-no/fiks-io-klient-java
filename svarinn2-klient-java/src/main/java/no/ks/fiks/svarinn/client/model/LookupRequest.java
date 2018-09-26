package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;

@Value
@Builder
public class LookupRequest {
    @NonNull private Identifikator identifikator;
    @NonNull private String dokumentType;
    @NonNull private Integer sikkerhetsNiva;
}
