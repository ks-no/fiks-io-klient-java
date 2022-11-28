package no.ks.fiks.io.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.client.model.*;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
class FiksIOHandler implements Closeable {
    private KontoId kontoId;
    private FiksIOSender utsendingKlient;
    private final KatalogHandler katalogHandler;
    private final AsicHandler asic;
    private final PublicKeyProvider publicKeyProvider;

    FiksIOHandler(@NonNull KontoId kontoId,
                  @NonNull FiksIOSender utsendingKlient,
                  @NonNull KatalogHandler katalogHandler,
                  @NonNull AsicHandler asicHandler,
                  @NonNull PublicKeyProvider publicKeyProvider) {
        this.kontoId = kontoId;

        this.utsendingKlient = utsendingKlient;
        this.katalogHandler = katalogHandler;
        this.asic = asicHandler;
        this.publicKeyProvider = publicKeyProvider;
    }

    SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return SendtMelding.fromSendResponse(getSend(request, payload));
    }

    SendtMelding sendRaw(@NonNull MeldingRequest request, @NonNull InputStream inputStream) {
        return SendtMelding.fromSendResponse(sendFerdigKryptertPakke(request, inputStream));
    }


    SvarSender buildKvitteringSender(@NonNull AmqpChannelFeedbackHandler amqpChannelFeedbackHandler, @NonNull MottattMelding melding) {
        return SvarSender.builder()
            .amqpChannelFeedbackHandler(amqpChannelFeedbackHandler)
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
            lagMeldingSpesifikasjon(request),
            Optional.of(payload).filter(p -> ! p.isEmpty()).map(p -> encrypt(p, request.getMottakerKontoId())));
    }

    private MeldingSpesifikasjonApiModel lagMeldingSpesifikasjon(@NonNull MeldingRequest request) {
        return MeldingSpesifikasjonApiModel.builder()
            .avsenderKontoId(kontoId.getUuid())
            .mottakerKontoId(request.getMottakerKontoId().getUuid())
            .meldingType(request.getMeldingType())
            .svarPaMelding(request.getSvarPaMelding() == null ? null : request.getSvarPaMelding()
                .getUuid())
            .ttl(Optional.ofNullable(request.getTtl()).map(Duration::toMillis).orElse(null))
            .headere(request.getHeadere())
            .build();
    }

    private SendtMeldingApiModel sendFerdigKryptertPakke(final MeldingRequest meldingRequest, final InputStream inputStream) {
        final UUID mottagerKontoId = meldingRequest.getMottakerKontoId()
            .getUuid();
        log.debug("Sender ferdig kryptert melding til \"{}\"", mottagerKontoId);
        return utsendingKlient.send(lagMeldingSpesifikasjon(meldingRequest), Optional.of(inputStream));
    }

    private InputStream encrypt(@NonNull final List<Payload> payload, final KontoId kontoId) {
        log.debug("Krypterer melding til konto \"{}\"", kontoId);
        return asic.encrypt(getPublicKey(kontoId), payload.stream().map(Payload::toContent).collect(Collectors.toList()));
    }

    private X509Certificate getPublicKey(final KontoId kontoId) {
        log.debug("Henter offentlig nøkkel for konto \"{}\"", kontoId);
        return publicKeyProvider.getPublicKey(kontoId);
    }

    @Override
    public void close() throws IOException {
        utsendingKlient.close();
    }
}
