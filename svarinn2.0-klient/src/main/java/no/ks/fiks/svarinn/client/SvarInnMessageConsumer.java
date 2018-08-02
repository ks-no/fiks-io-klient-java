package no.ks.fiks.svarinn.client;

public interface SvarInnMessageConsumer {

    void handleMessage(Melding melding);
}