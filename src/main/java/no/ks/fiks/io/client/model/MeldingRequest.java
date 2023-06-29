package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Value
@Builder
public class MeldingRequest implements MeldingSpesifikasjon {
    @NonNull private KontoId mottakerKontoId;
    @NonNull private String meldingType;
    @Nullable private Duration ttl;
    @Nullable private MeldingId svarPaMelding;
    @Nullable private MeldingId klientMeldingId;
    @Nullable private Map<String, String> headere;

    // Definerer denne for å hjelpe javadoc. Blir fylt ut av lombok pga Builder-annotasjon.
    public static class MeldingRequestBuilder {}

    public static MeldingRequestBuilder builder() {
        return new CustomMeldingRequestBuilder();
    }

    private static class CustomMeldingRequestBuilder extends MeldingRequestBuilder {

        @Override
        public MeldingRequest build() {
            final Map<String, String> copyHeadere;
            if(null == super.headere) {
                copyHeadere = new HashMap<>();
            } else {
                copyHeadere = new HashMap<>(super.headere);
            }

            final MeldingId klientMeldingId = super.klientMeldingId;
            if(null != klientMeldingId) {
                final String headerKlientMeldingId = copyHeadere.get(Melding.HeaderKlientMeldingId);

                if(null == headerKlientMeldingId) {
                    copyHeadere.put(Melding.HeaderKlientMeldingId, klientMeldingId.getUuid().toString());
                } else if(!Objects.equals(klientMeldingId.getUuid().toString(), headerKlientMeldingId)) {
                    throw new IllegalArgumentException(String.format("Property klientMeldingId er ulik %s-entry angitt via headere-property.", Melding.HeaderKlientMeldingId));
                }
            }

            super.headere = copyHeadere;

            return super.build();
        }
    }
}
