package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMeldingKonfigurasjon implements HostKonfigurasjon{

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
}
