package no.ks.fiks.svarinn.client;

import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.util.List;

class SvarInnHandler {
    private KontoId kontoId;
    private SvarInnUtsendingKlient utsendingKlient;
    private final KatalogHandler katalogHandler;
    private final AsicHandler asic;

    SvarInnHandler(@NonNull KontoId kontoId,
                   @NonNull SvarInnUtsendingKlient utsendingKlient,
                   @NonNull KatalogHandler katalogHandler,
                   @NonNull AsicHandler asicHandler) {
        this.kontoId = kontoId;

        this.utsendingKlient = utsendingKlient;
        this.katalogHandler = katalogHandler;
        this.asic = asicHandler;
    }

    SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return SendtMelding.fromSendResponse(utsendingKlient.send(
                MeldingSpesifikasjonApiModel.builder()
                        .avsenderKontoId(kontoId.getUuid())
                        .mottakerKontoId(request.getMottakerKontoId().getUuid())
                        .meldingType(request.getMeldingType())
                        .svarPaMelding(request.getSvarPaMelding() == null ? null : request.getSvarPaMelding().getUuid())
                        .ttl(request.getTtl().getSeconds())
                        .build(),
                payload.isEmpty() ? Option.none() : Option.some(asic.encrypt(katalogHandler.getPublicKey(request.getMottakerKontoId()), payload))));
    }

    KvitteringSender buildKvitteringSender(@NonNull Runnable acknowledge, @NonNull MottattMelding melding) {
        return KvitteringSender.builder()
                .doQueueAck(acknowledge)
                .utsendingKlient(utsendingKlient)
                .meldingSomSkalKvitteres(melding)
                .encrypt(payload -> asic.encrypt(katalogHandler.getPublicKey(melding.getAvsenderKontoId()), payload))
                .build();
    }
}
