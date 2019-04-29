package no.ks.fiks.io.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;
import org.eclipse.jetty.client.api.Request;

import java.util.function.Function;

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
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.scheme
     */
    private String scheme;

    /**
     * Ikke påkrevd felt. Gir mulighet for å intercepte requests mot dokumentlager-service
     */
    private Function<Request, Request> requestInterceptor;

}
