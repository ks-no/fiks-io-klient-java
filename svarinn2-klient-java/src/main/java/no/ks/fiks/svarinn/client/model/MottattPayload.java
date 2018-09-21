package no.ks.fiks.svarinn.client.model;

import lombok.Value;

@Value
public class MottattPayload {
    String filnavn;
    byte[] bytes;
}
