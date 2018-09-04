package no.ks.fiks.svarinn.client;

import no.ks.fiks.svarinn.client.model.Kvittering;

public interface SvarInnKvitteringConsumer {

    void handleKvittering(Kvittering kvittering);
}
