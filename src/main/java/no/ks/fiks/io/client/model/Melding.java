package no.ks.fiks.io.client.model;

import java.util.Map;

public interface Melding {
    MeldingId getMeldingId();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();

    String getMeldingType();

    Map<String, String> getHeadere();
}
