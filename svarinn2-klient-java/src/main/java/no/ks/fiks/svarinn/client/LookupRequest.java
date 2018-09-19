package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.Value;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;

@Value
@Builder
public class LookupRequest {
    private Identifikator identifikator;
    private String dokumentType;
    private Integer sikkerhetsNiva;

}
