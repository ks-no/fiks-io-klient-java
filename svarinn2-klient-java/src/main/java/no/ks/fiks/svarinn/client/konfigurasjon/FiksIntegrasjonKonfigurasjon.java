package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class FiksIntegrasjonKonfigurasjon {
    @NonNull private UUID integrasjonId;
    @NonNull private String integrasjonPassord;
    @NonNull private IdPortenKonfigurasjon idPortenKonfigurasjon;

}
