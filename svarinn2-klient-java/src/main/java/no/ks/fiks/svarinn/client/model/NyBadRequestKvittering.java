package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Value;
import no.ks.fiks.svarinn.client.KontoId;
import no.ks.fiks.svarinn.client.MeldingId;

import java.io.InputStream;
import java.time.Duration;

@Value
@Builder
public class NyBadRequestKvittering implements MeldingSpesifikasjon {
    private KontoId mottakerid;
    private MeldingId kvitteringForMelding;
    private InputStream melding;
}
