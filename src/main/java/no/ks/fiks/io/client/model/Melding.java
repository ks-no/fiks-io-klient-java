package no.ks.fiks.io.client.model;

public interface Melding {
    MeldingId getMeldingId();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();
}
