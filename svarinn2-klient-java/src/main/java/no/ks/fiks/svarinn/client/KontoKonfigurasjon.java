package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Value
@Builder
public class KontoKonfigurasjon {
    @NonNull KontoId kontoId;
    @NonNull PrivateKey privatNokkel;
}
