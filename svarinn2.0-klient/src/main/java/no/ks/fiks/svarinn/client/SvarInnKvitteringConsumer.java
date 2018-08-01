package no.ks.fiks.svarinn.client;

public interface SvarInnKvitteringConsumer {

    void handleKvittering(Kvittering kvittering);
}
