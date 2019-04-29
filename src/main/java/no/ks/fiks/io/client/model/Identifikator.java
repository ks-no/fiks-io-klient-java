package no.ks.fiks.io.client.model;

import lombok.Value;

@Value
public class Identifikator {
    IdentifikatorType identifikatorType;
    String identifikator;
}
