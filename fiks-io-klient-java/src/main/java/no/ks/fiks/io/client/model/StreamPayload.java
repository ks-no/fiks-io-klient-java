package no.ks.fiks.io.client.model;

import lombok.NonNull;
import no.ks.fiks.io.asice.model.Content;
import no.ks.fiks.io.asice.model.StreamContent;

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

    @Override
    public Content toContent() {
        return new StreamContent(getPayload(), getFilnavn());
    }
}
