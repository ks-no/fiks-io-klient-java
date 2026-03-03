package no.ks.fiks.io.client.send;

import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Optional;

/**
 * Standard grensesnitt for sending av melding til Fiks IO
 */
public interface FiksIOSender extends Closeable {

    /**
     * Sender melding
     * @param metadata beskriver metadata for meldingen
     * @param data payload til meldingen
     * @return metadata om meldingen som ble sendt
     */
    SendtMeldingApiModel send(MeldingSpesifikasjonApiModel metadata, Optional<InputStream> data);
}
