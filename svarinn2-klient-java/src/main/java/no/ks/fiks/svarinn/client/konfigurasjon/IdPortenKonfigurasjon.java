package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.Value;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Value
@Builder
public class IdPortenKonfigurasjon {
    private X509Certificate virksomhetsertifikat;
    private URI accessTokenUri;
    private PrivateKey privatNokkel;
    private String klientId;

}
