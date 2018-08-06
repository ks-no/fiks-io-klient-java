package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Melding {
    private final UUID meldingId;
    private final UUID avsenderId;
    private final UUID svarPaMelding;
    private final String meldingType;
    private final byte[] melding;
}
