package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;
import org.apache.hc.core5.http.ClassicHttpRequest;

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
     * Ikke påkrevd felt. Gir mulighet for å intercepte request mot fiksio-service.
     */
    private Function<ClassicHttpRequest, ClassicHttpRequest> requestInterceptor;
}
