package no.ks.fiks.io.client.eksempel.api;

import feign.FeignException;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.protokoll.konfigurasjon.v1.api.ProtokollKonfigurasjonApi;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.CreateProtokollKontoRequest;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.PartRequest;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.ProtokollKontoResponse;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.ProtokollSystemSummaryWithOrgNameResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Håndterer API-kall til Fiks Arkiv konfigurasjons-API for å opprette konto og administrere tilgang.
 */
public class FiksArkivApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(FiksArkivApiHandler.class);
    private final ProtokollKonfigurasjonApi protokollKonfigurasjonApi;

    public FiksArkivApiHandler(ProtokollKonfigurasjonKlient protokollKonfigurasjonKlient) {
        this.protokollKonfigurasjonApi = protokollKonfigurasjonKlient.protokollKonfigurasjonApi();
    }

    public ProtokollKontoResponse opprettFiksArkivKonto(UUID fiksOrgId, UUID systemId, String offentligNokkel) {
        logger.info("Oppretter ny Fiks Arkiv konto med part type 'no.ks.fiks.arkiv'");

        final var protokollNavn = "no.ks.fiks.arkiv";
        final var parts = List.of(arkivPart());

        final var request = new CreateProtokollKontoRequest()
            .navn("Fiks Arkiv Konto").parts(parts)
            .offentligNokkel(offentligNokkel)
            .beskrivelse("Konto for å sende meldinger til Arkiv")
            .stottetProtokollNavn(protokollNavn)
            .parts(parts);

        try {
            return protokollKonfigurasjonApi.createKonto(fiksOrgId, systemId, request);
        } catch (FeignException.BadRequest exception) {
            logger.error("Feil ved opprettelse av konto.\n {}", exception.contentUTF8());
            return null;
        }
    }

    public List<ProtokollSystemSummaryWithOrgNameResponse> seTilgangForesporsler(UUID fiksOrgId, UUID systemId, UUID kontoId) {
        return protokollKonfigurasjonApi.getForespurteTilgangerPaaKonto(fiksOrgId, systemId, kontoId);
    }

    private static PartRequest arkivPart() {
        return new PartRequest().partNavn("arkiv.full").stottetProtokollVersjon("v1");
    }

    public void beOmTilgang(UUID fiksOrgId, UUID systemId, KontoId eksternKonto) {
        logger.info("Ber om tilgang til å snakke med konto: {}", eksternKonto);

        protokollKonfigurasjonApi.createTilgangForesporselTilKonto(fiksOrgId, systemId, eksternKonto.getUuid());
    }

    public void godkjennTilgangsforesporsel(UUID fiksOrgId, UUID systemId, KontoId kontoId) {
        logger.info("Godkjenner alle tilgangsforespørsler på vegne av konto: {}", kontoId);

        var tilgangForespoersler = protokollKonfigurasjonApi.getForespurteTilgangerPaaKonto(fiksOrgId, systemId, kontoId.getUuid());
        tilgangForespoersler.forEach(system -> giTilgangTilSystem(fiksOrgId, systemId, kontoId.getUuid(), system));
    }

    private void giTilgangTilSystem(UUID fiksOrgId, UUID systemId, UUID kontoId, ProtokollSystemSummaryWithOrgNameResponse system) {
        protokollKonfigurasjonApi.createTilgangTilSystem(fiksOrgId, systemId, kontoId, system.getId());
    }
}

