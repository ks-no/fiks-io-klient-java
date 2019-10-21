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
    /**
     * Gir tilgang til avsenderkonto
     *
     * @return id på konto som brukes som avsenderkonto
     */
    KontoId getKontoId();

    /**
     * Brukes til å finne konto basert på adresse
     *
     * @param request parametre for kontooppslag {@link LookupRequest}
     * @return Optional som enten er tom eller inneholder et {@link Konto} objekt
     */
    Optional<Konto> lookup(LookupRequest request);

    /**
     * Lager Asic-E pakke, krypterer den og sender den via Fiks-IO
     *
     * @param request informasjon om mottager
     * @param payload liste av payloads som skal være en del av Asic-E pakken som sendes
     * @return kvittering på sending av melding
     */
    SendtMelding send(MeldingRequest request, List<Payload> payload);

    /**
     * Lager Asic-E pakke, krypterer den og sender den via Fiks-IO
     *
     * @param request informasjon om mottager
     * @param payload sti til fil som skal inngå i sendingen
     * @return kvittering på sending av melding
     */
    SendtMelding send(MeldingRequest request, Path payload);

    /**
     * Lager Asic-E pakke, krypterer den og sender den via Fiks-IO
     *
     * @param request informasjon om mottager
     * @param payload sti til fil som skal inngå i sendingen
     * @return kvittering på sending av melding
     */
    SendtMelding send(MeldingRequest request, String payload, String filnavn);

    /**
     * Lager Asic-E pakke, krypterer den og sender den via Fiks-IO
     *
     * @param request informasjon om mottager
     * @param payload {@link InputStream} som inneholder dataene som skal pakkes
     * @param filanvn navn på den endelige filen
     * @return kvittering på sending av melding
     */
    SendtMelding send(MeldingRequest request, InputStream payload, String filanvn);

    /**
     * Send ferdig kryptert Asic-E pakke. Ingen validering blir gjort av dataene slik at
     * avsender selv må ta ansvar for at dataene er pakket og kryptert riktig.
     *
     * @param meldingRequest  metadata for sending
     * @param kryptertPayload inputStream med ferdig krypterte data som skal være payload
     * @return kvittering på sending
     */
    SendtMelding sendAsiceInnhold(MeldingRequest meldingRequest, InputStream kryptertPayload);

    /**
     * Setter opp lytting på meldinger fra Fiks IO
     *
     * @param onMelding meldingshåndterer
     */
    void newSubscription(BiConsumer<MottattMelding, SvarSender> onMelding);

    /**
     * Setter opp lytting på meldinger fra Fiks IO
     *
     * @param onMelding meldingshåndterer
     * @param onClose   håndterer som kalles ved avslutining av klienten
     */
    void newSubscription(BiConsumer<MottattMelding, SvarSender> onMelding,
                         Consumer<ShutdownSignalException> onClose);
}
