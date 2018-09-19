package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Value;
import no.ks.fiks.svarinn.client.Identifikator;

@Builder
@Value
public class Lookup {
    private Identifikator identifikator;
    private String meldingType;
    private Integer sikkerhetsniva;
}
