package no.ks.fiks.io.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.client.model.*;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;
import org.jetbrains.annotations.NotNull;

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
    private final KontoId kontoId;
    private final FiksIOSender utsendingKlient;
    private final AsicHandler asic;


    private final LoadingCache<KontoId, X509Certificate> publicKeyCache;

    FiksIOHandler(@NonNull KontoId kontoId,
                  @NonNull FiksIOSender utsendingKlient,
                  @NonNull AsicHandler asicHandler,
                  @NonNull PublicKeyProvider publicKeyProvider) {
        this.kontoId = kontoId;

        this.utsendingKlient = utsendingKlient;
        this.asic = asicHandler;

        publicKeyCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(100)
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public X509Certificate load(@NotNull KontoId kontoId) {
                    log.info("Henter public key eksternt for konto {}", kontoId.getUuid());
                    return publicKeyProvider.getPublicKey(kontoId);
                }
            });
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
        final long lagSpesifikkasjonStart = System.currentTimeMillis();
        MeldingSpesifikasjonApiModel metadata = lagMeldingSpesifikasjon(request);
        log.info("Laget spesifikkasjon på {}ms", System.currentTimeMillis()-lagSpesifikkasjonStart);
        Optional<InputStream> data = Optional.of(payload).filter(p -> !p.isEmpty()).map(p -> encrypt(p, request.getMottakerKontoId()));
        final long startSend = System.currentTimeMillis();
        SendtMeldingApiModel send = utsendingKlient.send(
            metadata,
            data);
        log.info("Sendte melding på {}ms", System.currentTimeMillis()-startSend);
        return send;
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
        final long startEncrypt = System.currentTimeMillis();
        InputStream encrypt = asic.encrypt(getPublicKey(kontoId), payload.stream().map(Payload::toContent).collect(Collectors.toList()));
        log.info("Krypterte melding på {}ms", System.currentTimeMillis()-startEncrypt);
        return encrypt;
    }

    private X509Certificate getPublicKey(final KontoId kontoId) {
        log.info("Henter offentlig nøkkel for konto fra cache \"{}\"", kontoId);
        final long startHentKey = System.currentTimeMillis();
        X509Certificate unchecked = publicKeyCache.getUnchecked(kontoId);
        log.info("Hentet nøkkel på {}ms", System.currentTimeMillis()-startHentKey);
        return unchecked;
    }

    @Override
    public void close() throws IOException {
        utsendingKlient.close();
    }
}
