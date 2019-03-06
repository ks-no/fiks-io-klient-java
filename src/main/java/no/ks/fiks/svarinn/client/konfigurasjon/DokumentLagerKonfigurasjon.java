package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;

/**
 * Konfigurer klienten som benyttes for å lagre og hente dokumenter fra fiks-dokumentlager. Dokumentlager benyttes for å lagre meldinger som er større enn maks-størrelse for amqp meldinger.
 */
@Data
@Builder()
public class DokumentLagerKonfigurasjon implements HostKonfigurasjon{

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.host
     */
    private String host;

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.port
     */
    private Integer port;

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.sheme
     */
    private String scheme;

    public String getUrl() {
        return String.format("%s://%s:%s", getScheme(), getHost(), getPort());
    }
}
