package no.ks.fiks.svarinn.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.Melding;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Value
@Builder
public class SubscribeSettings {
    @NonNull private BiConsumer<Melding, KvitteringSender> onMelding;
    @Builder.Default private Consumer<Exception> onClose = p -> {};
}
