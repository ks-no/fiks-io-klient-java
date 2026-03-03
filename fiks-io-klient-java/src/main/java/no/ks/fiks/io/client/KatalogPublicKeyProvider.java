package no.ks.fiks.io.client;

import no.ks.fiks.io.client.model.KontoId;

import java.security.cert.X509Certificate;
import java.util.UUID;

public class KatalogPublicKeyProvider implements PublicKeyProvider {

    final private KatalogHandler katalogHandler;

    public KatalogPublicKeyProvider(KatalogHandler katalogHandler) {
        this.katalogHandler = katalogHandler;
    }

    @Override
    public X509Certificate getPublicKey(KontoId kontoId) {
        return katalogHandler.getPublicKey(kontoId);
    }

}
