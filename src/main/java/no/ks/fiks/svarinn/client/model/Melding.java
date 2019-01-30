package no.ks.fiks.svarinn.client.model;

public interface Melding {
    MeldingId getMeldingId();

    String getMeldingType();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();
}
