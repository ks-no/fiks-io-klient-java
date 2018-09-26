package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.KontoId;

import java.security.PrivateKey;

@Value
@Builder
public class KontoKonfigurasjon {
    @NonNull
    KontoId kontoId;
    @NonNull PrivateKey privatNokkel;
}
