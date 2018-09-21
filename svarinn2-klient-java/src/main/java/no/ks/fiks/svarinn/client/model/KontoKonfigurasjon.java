package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.PrivateKey;

@Value
@Builder
public class KontoKonfigurasjon {
    @NonNull
    KontoId kontoId;
    @NonNull PrivateKey privatNokkel;
}
