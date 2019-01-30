package no.ks.fiks.svarinn.client.model;

import java.io.InputStream;

public interface Payload {
    String getFilnavn();
    InputStream getPayload();

}
