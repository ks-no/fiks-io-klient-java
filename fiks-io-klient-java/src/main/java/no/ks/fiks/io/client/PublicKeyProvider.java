package no.ks.fiks.io.client;

import no.ks.fiks.io.client.model.KontoId;

import java.security.cert.X509Certificate;

public interface PublicKeyProvider {

    X509Certificate getPublicKey(final KontoId kontoId);

}
