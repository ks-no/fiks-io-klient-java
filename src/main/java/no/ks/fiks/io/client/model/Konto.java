package no.ks.fiks.io.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto;

@Builder
@Value
public class Konto {
    @NonNull private KontoId kontoId;
    @NonNull private String kontoNavn;
    @NonNull private FiksOrgId fiksOrgId;
    @NonNull private String fiksOrgNavn;
    private String kommuneNummer;
    private boolean isGyldigAvsender;
    private boolean isGyldigMottaker;

    public static Konto fromKatalogModel(@NonNull KatalogKonto konto) {
        return Konto.builder()
            .kontoId(new KontoId(konto.getKontoId()))
            .kontoNavn(konto.getKontoNavn())
            .fiksOrgId(new FiksOrgId(konto.getFiksOrgId()))
            .fiksOrgNavn(konto.getFiksOrgNavn())
            .kommuneNummer(konto.getKommuneNummer())
            .isGyldigAvsender(konto.getStatus().getGyldigAvsender())
            .isGyldigMottaker(konto.getStatus().getGyldigMottaker())
            .build();
    }
}
