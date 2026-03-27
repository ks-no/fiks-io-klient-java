package no.ks.fiks.io.client.model;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

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
    @Nullable private KlientKorrelasjonId korrelasjonsId;

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

            final KlientKorrelasjonId korrelasjonsId = super.korrelasjonsId;
            if(null != korrelasjonsId) {
                final String headerKorrelasjonsId = getKlientKorrelasjonsId(copyHeadere);

                if(null == headerKorrelasjonsId) {
                    leggTilKorrelasjonsId(copyHeadere, korrelasjonsId);
                } else if(!Objects.equals(korrelasjonsId.getKlientKorrelasjonId(), headerKorrelasjonsId)) {
                    throw new IllegalArgumentException(String.format("Property korrelasjonsId er ulik %s-entry angitt via headere-property.", Melding.HeaderKlientKorrelasjonId));
                }
            }

            super.headere = copyHeadere;

            return super.build();
        }

        private static void leggTilKorrelasjonsId(Map<String, String> copyHeadere, KlientKorrelasjonId korrelasjonsId) {
            copyHeadere.put(Melding.HeaderKlientKorrelasjonIdDeprecated, korrelasjonsId.getKlientKorrelasjonId());
            copyHeadere.put(Melding.HeaderKlientKorrelasjonId, korrelasjonsId.getKlientKorrelasjonId());
        }

        private static String getKlientKorrelasjonsId(Map<String, String> copyHeadere) {
            var headerKorrelasjonsId = copyHeadere.get(Melding.HeaderKlientKorrelasjonId);
            var oldHeaderKorrelasjonsId = copyHeadere.get(Melding.HeaderKlientKorrelasjonIdDeprecated);

            return (headerKorrelasjonsId != null) ? headerKorrelasjonsId : oldHeaderKorrelasjonsId;
        }
    }
}
