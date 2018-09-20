package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.ShutdownSignalException;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.MottattMelding;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Value
@Builder
public class SubscribeSettings {
    @NonNull private BiConsumer<MottattMelding, KvitteringSender> onMelding;
    @Builder.Default private Consumer<ShutdownSignalException> onClose = p -> {};
}
