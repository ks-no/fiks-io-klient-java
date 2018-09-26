package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class Konto {
    @NonNull private KontoId kontoId;
    @NonNull private String kontoNavn;
    @NonNull private FiksOrgId fiksOrgId;
    @NonNull private String fiksOrgNavn;
    private boolean aktiv;

    public static Konto fromKatalogModel(@NonNull no.ks.fiks.svarinn2.katalog.swagger.model.v1.Konto konto){
        return Konto.builder()
                .kontoId(new KontoId(konto.getKontoId()))
                .kontoNavn(konto.getKontoNavn())
                .fiksOrgId(new FiksOrgId(konto.getFiksOrgId()))
                .fiksOrgNavn(konto.getKontoNavn())
                .aktiv(konto.isAktiv())
                .build();
    }
}
