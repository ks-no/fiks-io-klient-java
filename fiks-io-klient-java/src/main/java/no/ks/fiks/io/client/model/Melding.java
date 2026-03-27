package no.ks.fiks.io.client.model;

import java.util.Map;

public interface Melding {

    static final String HeaderKlientMeldingId = "klientMeldingId";
    static final String HeaderKlientKorrelasjonId = "klientKorrelasjonsId" ;
    @Deprecated
    static final String HeaderKlientKorrelasjonIdDeprecated = "klientKorrelasjonId" ;

    MeldingId getMeldingId();

    KontoId getAvsenderKontoId();

    KontoId getMottakerKontoId();

    java.time.Duration getTtl();

    MeldingId getSvarPaMelding();

    String getMeldingType();

    Map<String, String> getHeadere();

    MeldingId getKlientMeldingId();

    KlientKorrelasjonId getKlientKorrelasjonId();
}
