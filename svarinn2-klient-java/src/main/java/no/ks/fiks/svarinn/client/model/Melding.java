package no.ks.fiks.svarinn.client.model;

public interface Melding {
    no.ks.fiks.svarinn.client.MeldingId getMeldingId();

    String getMeldingType();

    no.ks.fiks.svarinn.client.KontoId getAvsenderKontoId();

    no.ks.fiks.svarinn.client.KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    no.ks.fiks.svarinn.client.MeldingId getSvarPaMelding();
}
