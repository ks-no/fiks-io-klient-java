package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Melding {
    private final UUID korrelasjonId;
    private final UUID avsenderId;
    private final String meldingType;
    private final byte[] melding;
}
