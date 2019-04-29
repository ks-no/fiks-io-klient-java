package no.ks.fiks.io.client.model;

import java.io.InputStream;

public interface Payload {
    String getFilnavn();
    InputStream getPayload();

}
