package no.ks.fiks.io.client.model;

import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.util.Map;
import java.util.UUID;

public interface Melding {

    static final String HeaderKlientMeldingId = "klientMeldingId";

    MeldingId getMeldingId();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();

    String getMeldingType();

    Map<String, String> getHeadere();

    MeldingId getKlientMeldingId();
}
