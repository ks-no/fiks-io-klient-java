package no.ks.fiks.io.client.model;

import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StringPayload implements Payload {
    private String payload;
    private String filnavn;

    public StringPayload(@NonNull String payload, @NonNull String filnavn) {
        this.payload = payload;
        this.filnavn = filnavn;
    }

    @Override
    public String getFilnavn() {
        return filnavn;
    }

    @Override
    public InputStream getPayload() {
        return new ByteArrayInputStream(payload.getBytes());
    }
}
