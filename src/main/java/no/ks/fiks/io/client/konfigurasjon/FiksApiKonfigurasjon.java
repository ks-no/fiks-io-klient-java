package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FiksApiKonfigurasjon implements HostKonfigurasjon{

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes "api.fiks.ks.no"
     */
    @Builder.Default private String host = "api.fiks.ks.no";

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes "443"
     */
    @Builder.Default private Integer port = 443;

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes "https"
     */
    @Builder.Default private String scheme = "https";
}
