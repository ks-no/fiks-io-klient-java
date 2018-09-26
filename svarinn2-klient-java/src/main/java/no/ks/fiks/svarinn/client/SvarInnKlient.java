package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.ShutdownSignalException;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.konfigurasjon.SvarInnKonfigurasjon;
import no.ks.fiks.svarinn.client.model.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class SvarInnKlient {

    private final KontoId kontoId;
    private final MeldingHandler meldingHandler;
    private final KatalogHandler katalogHandler;
    private final SvarInnHandler svarInnHandler;

    public SvarInnKlient(@NonNull SvarInnKonfigurasjon settings) {
        kontoId = settings.getKontoKonfigurasjon().getKontoId();
        katalogHandler = new KatalogHandler(settings.getKatalogApi());
        AsicHandler asicHandler = new AsicHandler(settings.getKontoKonfigurasjon().getPrivatNokkel());
        svarInnHandler = new SvarInnHandler(kontoId, settings.getSvarInn2Api(), katalogHandler, asicHandler);
        meldingHandler = new MeldingHandler(settings.getAmqpKonfigurasjon(), settings.getFiksIntegrasjonKonfigurasjon(), svarInnHandler, asicHandler, kontoId);
    }

    public KontoId getKontoId() {
        return kontoId;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        return katalogHandler.lookup(request);
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return svarInnHandler.send(request, payload);
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload) {
        return send(request, singletonList(new StringPayload(payload, "payload.txt")));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull File payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    public void newSubscription(@NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding) {
        newSubscription(onMelding, p -> {
        });
    }

    public void newSubscription(@NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        meldingHandler.newConsume(onMelding, onClose);
    }

}
