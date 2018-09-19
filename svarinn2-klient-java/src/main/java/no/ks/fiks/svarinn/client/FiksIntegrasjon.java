package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.Value;

import java.io.File;
import java.util.UUID;

@Value
@Builder
public class FiksIntegrasjon {
    private UUID integrasjonId;
    private String integrasjonPassord;
    private String virksomhetsertifikat;
}
