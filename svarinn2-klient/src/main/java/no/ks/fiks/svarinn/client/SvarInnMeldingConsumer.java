package no.ks.fiks.svarinn.client;

import no.ks.fiks.svarinn.client.model.Melding;

public interface SvarInnMeldingConsumer {

    void handleMessage(Melding melding);
}