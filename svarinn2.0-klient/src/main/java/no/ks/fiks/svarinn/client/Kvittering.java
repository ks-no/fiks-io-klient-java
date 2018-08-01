package no.ks.fiks.svarinn.client;

import lombok.Data;

import java.util.UUID;

@Data
public class Kvittering {
    private final UUID avsenderId;
    private final UUID korrelasjonId;
    private final String type;
    private final byte[] body;
}
