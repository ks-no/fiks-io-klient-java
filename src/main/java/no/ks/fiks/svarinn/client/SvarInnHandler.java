package no.ks.fiks.svarinn.client;

import io.vavr.control.Option;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.svarinn2.klient.SendtMeldingApiModel;
import no.ks.fiks.svarinn2.klient.SvarInnUtsendingKlient;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

@Slf4j
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
        return SendtMelding.fromSendResponse(getSend(request, payload));
    }

    SvarSender buildKvitteringSender(@NonNull Runnable acknowledge, @NonNull MottattMelding melding) {
        return SvarSender.builder()
            .doQueueAck(acknowledge)
            .utsendingKlient(utsendingKlient)
            .meldingSomSkalKvitteres(melding)
            .encrypt(payload -> encrypt(payload, melding.getAvsenderKontoId()))
            .build();
    }

    private SendtMeldingApiModel getSend(@NonNull final MeldingRequest request, @NonNull final List<Payload> payload) {
        final UUID mottagerKontoId = request.getMottakerKontoId()
            .getUuid();
        log.debug("Sender melding til \"{}\"", mottagerKontoId);
        return utsendingKlient.send(
            MeldingSpesifikasjonApiModel.builder()
                .avsenderKontoId(kontoId.getUuid())
                .mottakerKontoId(mottagerKontoId)
                .meldingType(request.getMeldingType())
                .svarPaMelding(request.getSvarPaMelding() == null ? null : request.getSvarPaMelding()
                    .getUuid())
                .ttl(request.getTtl()
                    .getSeconds())
                .build(),
            payload.isEmpty() ? Option.none() : Option.some(encrypt(payload, request.getMottakerKontoId())));
    }

    private InputStream encrypt(@NonNull final List<Payload> payload, final KontoId kontoId) {
        log.debug("Krypterer melding til konto \"{}\"", kontoId);
        return asic.encrypt(getPublicKey(kontoId), payload);
    }

    private X509Certificate getPublicKey(final KontoId kontoId) {
        log.debug("Henter offentlig nøkkel for konto \"{}\"", kontoId);
        return katalogHandler.getPublicKey(kontoId);
    }
}
