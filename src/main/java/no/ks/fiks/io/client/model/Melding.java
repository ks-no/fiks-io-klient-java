package no.ks.fiks.io.client.model;

public interface Melding {
    MeldingId getMeldingId();

    String getMeldingType();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();
}
