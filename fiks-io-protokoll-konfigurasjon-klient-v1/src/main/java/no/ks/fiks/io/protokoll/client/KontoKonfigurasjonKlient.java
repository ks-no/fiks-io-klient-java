package no.ks.fiks.io.protokoll.client;

import feign.Feign;
import feign.hc5.ApacheHttp5Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.protokoll.konfigurasjon.api.v1.api.ProtokollKonfigurasjonApi;
import no.ks.fiks.protokoll.konfigurasjon.api.v1.invoker.ApiClient;

import java.util.UUID;
import java.util.function.Supplier;

public class KontoKonfigurasjonKlient {
    private static final String REQUEST_ID_HEADER = "requestId";

    static public ProtokollKonfigurasjonApi createKlient(
            String hostUrl,
            UUID integrasjonId,
            String integrasjonPassord,
            Supplier<String> maskinportenTokenSupplier) {
        ApiClient apiClient = new ApiClient();
        return Feign.builder()
            .client(new ApacheHttp5Client())
            .encoder(new JacksonEncoder(apiClient.getObjectMapper()))
            .decoder(new JacksonDecoder(apiClient.getObjectMapper()))
            .requestInterceptor(RequestInterceptors.integrasjon(integrasjonId, integrasjonPassord))
            .requestInterceptor(RequestInterceptors.accessToken(maskinportenTokenSupplier))
            .requestInterceptor(t -> t.header(REQUEST_ID_HEADER, UUID.randomUUID().toString()))
            .target(ProtokollKonfigurasjonApi.class, hostUrl);
    }
}
