package no.ks.fiks.svarinn.client.konfigurasjon;

import feign.RequestInterceptor;
import lombok.Builder;
import lombok.Data;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;

import java.util.function.Function;

@Data
@Builder
public class KatalogKonfigurasjon implements HostKonfigurasjon {

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
     * Ikke påkrevd. Gir mulighet til å benytte en cache for oppslag mot svarinn katalogen for å bedre ytelse. Hvis dette ikke settes vil ikke caching bli benyttet
     */
    private Function<LookupRequest, KontoId> katalogApiCache;

    /**
     * Ikke påkrevd. Gir mulighet til å intercepte requests mot katalog-service
     */
    private RequestInterceptor requestInterceptor;



}
