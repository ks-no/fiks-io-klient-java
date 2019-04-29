package no.ks.fiks.io.client.send;

import io.vavr.control.Option;
import no.ks.fiks.svarinn2.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.svarinn2.klient.SendtMeldingApiModel;

import java.io.InputStream;

public interface FiksIOSender {

    SendtMeldingApiModel send(MeldingSpesifikasjonApiModel metadata, Option<InputStream> data);
}
