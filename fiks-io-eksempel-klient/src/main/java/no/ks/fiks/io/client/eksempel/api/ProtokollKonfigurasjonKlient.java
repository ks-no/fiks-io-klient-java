package no.ks.fiks.io.client.eksempel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.hc5.ApacheHttp5Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import no.ks.fiks.feign.RequestInterceptors;
import no.ks.fiks.io.client.eksempel.config.FiksApiProperties;
import no.ks.fiks.io.client.eksempel.utils.TokenProvider;
import no.ks.fiks.protokoll.konfigurasjon.v1.api.ProtokollKonfigurasjonApi;
import no.ks.fiks.protokoll.konfigurasjon.v1.invoker.ApiClient;

import java.util.UUID;

public class ProtokollKonfigurasjonKlient {
    private final ObjectMapper OBJECT_MAPPER = new ApiClient().getObjectMapper();
    private final String REQUEST_ID = "requestId";

    private final TokenProvider tokenProvider;
    private final UUID integrasjonId;
    private final String integrasjonPassord;
    private final String hostUrl;

    public ProtokollKonfigurasjonKlient(TokenProvider tokenProvider, UUID integrasjonId, String integrasjonPassord, FiksApiProperties fiksApiProperties) {
        this.tokenProvider = tokenProvider;
        this.integrasjonId = integrasjonId;
        this.integrasjonPassord = integrasjonPassord;
        this.hostUrl = String.format("%s://%s:%s", fiksApiProperties.scheme(), fiksApiProperties.host(), fiksApiProperties.port());
    }

    public ProtokollKonfigurasjonApi protokollKonfigurasjonApi() {
        return new Feign.Builder()
            .client(new ApacheHttp5Client())
            .encoder(new JacksonEncoder(OBJECT_MAPPER))
            .decoder(new JacksonDecoder(OBJECT_MAPPER))
            .requestInterceptor(RequestInterceptors.integrasjon(integrasjonId, integrasjonPassord))
            .requestInterceptor(RequestInterceptors.accessToken(tokenProvider::getMaskinportenToken))
            .requestInterceptor(requestTemplate -> requestTemplate.header(REQUEST_ID, UUID.randomUUID().toString()))
            .target(ProtokollKonfigurasjonApi.class, hostUrl);
    }
}
