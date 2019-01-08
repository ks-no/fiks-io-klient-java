package no.ks.fiks.svarinn.client.model;

import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StreamPayload implements Payload {
    private InputStream payload;
    private String filnavn;

    public StreamPayload(@NonNull InputStream payload, @NonNull String filnavn) {
        this.payload = payload;
        this.filnavn = filnavn;
    }

    @Override
    public String getFilnavn() {
        return filnavn;
    }

    @Override
    public InputStream getPayload() {
        return payload;
    }
}
