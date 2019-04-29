package no.ks.fiks.io.client;

import com.rabbitmq.client.ShutdownSignalException;
import no.ks.fiks.io.client.model.*;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FiksIOKlient extends Closeable {
    KontoId getKontoId();

    Optional<Konto> lookup(LookupRequest request);

    SendtMelding send(MeldingRequest request, List<Payload> payload);

    SendtMelding send(MeldingRequest request, Path payload);

    SendtMelding send(MeldingRequest request, String payload, String filnavn);

    SendtMelding send(MeldingRequest request, InputStream payload, String filanvn);

    void newSubscription(BiConsumer<MottattMelding, SvarSender> onMelding);

    void newSubscription(BiConsumer<MottattMelding, SvarSender> onMelding,
                         Consumer<ShutdownSignalException> onClose);
}
