package no.ks.fiks.svarinn.client.model;

import lombok.Data;
import no.ks.fiks.klient.svarinn2.model.v1.BadRequestKvittering;
import no.ks.fiks.klient.svarinn2.model.v1.ExpiredKvittering;
import no.ks.fiks.klient.svarinn2.model.v1.FeiletKvittering;
import no.ks.fiks.klient.svarinn2.model.v1.MottattKvittering;

import java.util.UUID;

@Data
public class Kvittering {
    private final UUID avsenderId;
    private final UUID kvitteringForMeldingId;
    private final String type;
    private MottattKvittering mottattKvittering;
    private BadRequestKvittering badRequestKvittering;
    private FeiletKvittering feiletKvittering;
    private ExpiredKvittering expiredKvittering;
}
