package no.ks.fiks.svarinn.client;

import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.util.List;

class SvarInnHandler {
    private KontoId kontoId;
    private SvarInnApi svarInnApi;
    private final KatalogHandler katalogHandler;
    private final AsicHandler asic;

    SvarInnHandler(@NonNull KontoId kontoId,
                   @NonNull SvarInnApi svarInnApi,
                   @NonNull KatalogHandler katalogHandler,
                   @NonNull AsicHandler asicHandler) {
        this.kontoId = kontoId;

        this.svarInnApi = svarInnApi;
        this.katalogHandler = katalogHandler;
        this.asic = asicHandler;
    }

    SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return SendtMelding.fromSendResponse(svarInnApi.sendMelding(
                kontoId.toString(),
                request.getMottakerKontoId().toString(),
                request.getMeldingType(),
                asic.encrypt(katalogHandler.getPublicKey(request.getMottakerKontoId()), payload),
                Option.of(request.getSvarPaMelding()).map(MeldingId::toString).getOrElse(() -> null),
                request.getTtl().toMillis()));
    }

    KvitteringSender buildKvitteringSender(@NonNull Runnable acknowledge, @NonNull MottattMelding melding) {
        return KvitteringSender.builder()
                .doQueueAck(acknowledge)
                .svarInnApi(svarInnApi)
                .meldingSomSkalKvitteres(melding)
                .encrypt(payload -> asic.encrypt(katalogHandler.getPublicKey(melding.getAvsenderKontoId()), payload))
                .build();
    }
}
