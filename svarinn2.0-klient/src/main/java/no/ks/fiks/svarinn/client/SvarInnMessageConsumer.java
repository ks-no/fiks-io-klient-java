package no.ks.fiks.svarinn.client;

import no.ks.fiks.klient.mottak.model.v1.Melding;

public interface SvarInnMessageConsumer {

    void handleMessage(Melding melding);
}