package no.ks.fiks.svarinn.client.model;

import lombok.Value;

@Value
public class Identifikator {
    IdentifikatorType identifikatorType;
    String identifikator;
}
