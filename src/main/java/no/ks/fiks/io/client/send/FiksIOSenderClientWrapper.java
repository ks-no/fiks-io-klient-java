package no.ks.fiks.io.client.send;

import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.io.klient.FiksIOUtsendingKlient;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.IOException;
import java.io.InputStream;

public class FiksIOSenderClientWrapper implements FiksIOSender {

    private final FiksIOUtsendingKlient utsendingKlient;

    public FiksIOSenderClientWrapper(@NonNull final FiksIOUtsendingKlient utsendingKlient) {
        this.utsendingKlient = utsendingKlient;
    }

    @Override
    public SendtMeldingApiModel send(final MeldingSpesifikasjonApiModel metadata, final Option<InputStream> data) {
        return utsendingKlient.send(metadata, data);
    }

    @Override
    public void close() throws IOException {
        utsendingKlient.close();
    }
}
