package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;
import org.eclipse.jetty.client.api.Request;

import java.util.function.Function;

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
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.scheme
     */
    private String scheme;

    /**
     * Ikke påkrevd felt. Gir mulighet for å intercepte request mot svarinn2-send-service.
     */
    private Function<Request, Request> requestInterceptor;
}
