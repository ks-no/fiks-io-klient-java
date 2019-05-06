package no.ks.fiks.io.client.send;

import io.vavr.control.Option;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;

import java.io.InputStream;

public interface FiksIOSender {

    SendtMeldingApiModel send(MeldingSpesifikasjonApiModel metadata, Option<InputStream> data);
}
