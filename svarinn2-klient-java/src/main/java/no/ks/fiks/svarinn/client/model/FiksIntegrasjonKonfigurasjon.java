package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Value;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.UUID;

@Value
@Builder
public class FiksIntegrasjonKonfigurasjon {
    private UUID integrasjonId;
    private String integrasjonPassord;
    private X509Certificate virksomhetsertifikat;
}
