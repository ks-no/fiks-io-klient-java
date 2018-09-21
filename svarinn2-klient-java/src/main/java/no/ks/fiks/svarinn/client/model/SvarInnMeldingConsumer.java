package no.ks.fiks.svarinn.client.model;

import no.ks.fiks.svarinn.client.model.MeldingSpesifikasjon;

public interface SvarInnMeldingConsumer {

    void vedNyMelding(MeldingSpesifikasjon melding);

    void vedAvbrudd(MeldingSpesifikasjon melding);
}