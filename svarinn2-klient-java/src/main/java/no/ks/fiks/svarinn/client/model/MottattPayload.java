package no.ks.fiks.svarinn.client.model;

import lombok.NonNull;
import lombok.Value;

@Value
public class MottattPayload {
    @NonNull String filnavn;
    @NonNull byte[] bytes;
}
