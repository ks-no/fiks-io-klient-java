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
    String orgNummer;
    String kommuneNummer;
    boolean isGyldigAvsender;
    boolean isGyldigMottaker;
    long antallKonsumenter;
    long antallUavhentedeMeldinger;

    public static Konto fromKatalogModel(@NonNull KatalogKonto konto) {
        return Konto.builder()
            .kontoId(new KontoId(konto.getKontoId()))
            .kontoNavn(konto.getKontoNavn())
            .fiksOrgId(new FiksOrgId(konto.getFiksOrgId()))
            .fiksOrgNavn(konto.getFiksOrgNavn())
            .orgNummer(konto.getOrganisasjonsnummer())
            .kommuneNummer(konto.getKommunenummer())
            .isGyldigAvsender(konto.getStatus().getGyldigAvsender())
            .isGyldigMottaker(konto.getStatus().getGyldigMottaker())
            .antallKonsumenter(konto.getStatus().getAntallKonsumenter())
            .antallUavhentedeMeldinger(konto.getStatus().getAntallUavhentedeMeldinger())
            .build();
    }
}
