package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.util.function.Function;

@Value
@Builder
public class SvarInnKonfigurasjon {
    @NonNull private KontoKonfigurasjon kontoKonfigurasjon;
    @NonNull private AmqpKonfigurasjon amqpKonfigurasjon;
    @NonNull private FiksIntegrasjonKonfigurasjon fiksIntegrasjonKonfigurasjon;

    @NonNull private SvarInnApi svarInn2Api;
    @NonNull private SvarInnKatalogApi katalogApi;
    private Function<LookupRequest, KontoId> lookupCache;
}
