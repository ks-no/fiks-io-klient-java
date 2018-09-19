package no.ks.fiks.svarinn.client.model;

import lombok.Builder;
import lombok.Value;
import no.ks.fiks.svarinn.client.KontoId;
import no.ks.fiks.svarinn.client.MeldingId;

import java.io.InputStream;

@Value
@Builder
public class NyMottatttKvittering implements MeldingSpesifikasjon {
    private KontoId mottakerid;
    private MeldingId kvitteringForMelding;
}
