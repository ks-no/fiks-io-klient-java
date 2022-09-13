package no.ks.fiks.io.client.send;

import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Optional;

public interface FiksIOSender extends Closeable {

    SendtMeldingApiModel send(MeldingSpesifikasjonApiModel metadata, Optional<InputStream> data);
}
