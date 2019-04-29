package no.ks.fiks.io.client;

import com.rabbitmq.client.ShutdownSignalException;
import lombok.NonNull;
import no.ks.fiks.io.client.model.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class FiksIOKlientImpl implements FiksIOKlient {

    private final KontoId kontoId;
    private final AmqpHandler amqpHandler;
    private final KatalogHandler katalogHandler;
    private final FiksIOHandler fiksIOHandler;

    public FiksIOKlientImpl(@NonNull KontoId kontoId, @NonNull AmqpHandler amqpHandler, @NonNull KatalogHandler katalogHandler, @NonNull FiksIOHandler fiksIOHandler) {
        this.kontoId = kontoId;
        this.amqpHandler = amqpHandler;
        this.katalogHandler = katalogHandler;
        this.fiksIOHandler = fiksIOHandler;
    }

    @Override
    public KontoId getKontoId() {
        return kontoId;
    }

    @Override
    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        return katalogHandler.lookup(request);
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return fiksIOHandler.send(request, payload);
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull Path payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload, @NonNull String filnavn) {
        return send(request, singletonList(new StringPayload(payload, filnavn)));
    }

    @Override
    public SendtMelding send(@NonNull MeldingRequest request, @NonNull InputStream payload, @NonNull String filanvn) {
        return send(request, singletonList(new StreamPayload(payload, filanvn)));
    }

    @Override
    public void newSubscription(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding) {
        newSubscription(onMelding, p -> {});
    }

    @Override
    public void newSubscription(@NonNull BiConsumer<MottattMelding, SvarSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        amqpHandler.newConsume(onMelding, onClose);
    }

    public void close() {
        //TODO close-it
    }
}
