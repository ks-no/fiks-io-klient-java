package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto;

@Builder
@Value
public class Konto {
    @NonNull KontoId kontoId;
    @NonNull String kontoNavn;
    @NonNull FiksOrgId fiksOrgId;
    @NonNull String fiksOrgNavn;
    @NonNull String orgNummer;
    String kommuneNummer;
    boolean isGyldigAvsender;
    boolean isGyldigMottaker;

    public static Konto fromKatalogModel(@NonNull KatalogKonto konto) {
        return Konto.builder()
            .kontoId(new KontoId(konto.getKontoId()))
            .kontoNavn(konto.getKontoNavn())
            .fiksOrgId(new FiksOrgId(konto.getFiksOrgId()))
            .fiksOrgNavn(konto.getFiksOrgNavn())
            .orgNummer(konto.getOrganisasjonsnummer())
            .kommuneNummer(konto.getKommuneNummer())
            .isGyldigAvsender(konto.getStatus().getGyldigAvsender())
            .isGyldigMottaker(konto.getStatus().getGyldigMottaker())
            .build();
    }
}
