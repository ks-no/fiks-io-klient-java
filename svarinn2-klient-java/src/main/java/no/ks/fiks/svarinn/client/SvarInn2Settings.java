package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.function.Function;

@Value
@Builder
public class SvarInn2Settings {
    @NonNull private Channel rabbitMqChannel;
    @NonNull private KontoId kontoId;
    @NonNull private FiksIntegrasjon fiksIntegrasjon;
    @NonNull private SvarInnApi svarInn2Api;
    @NonNull private SvarInnKatalogApi katalogApi;
    @NonNull private X509Certificate sertifikat;
    @NonNull private PrivateKey privatNokkel;

    private Function<LookupRequest, KontoId> lookupCache;
}
