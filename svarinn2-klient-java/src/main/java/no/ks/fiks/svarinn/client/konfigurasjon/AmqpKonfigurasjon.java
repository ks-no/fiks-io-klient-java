package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.MeldingId;

import java.util.function.Predicate;

@Value
@Builder
public class AmqpKonfigurasjon {
    @NonNull private String host;
    @NonNull private Integer port;
    @Builder.Default private Predicate<MeldingId> meldingErBehandlet = m -> true;
}
