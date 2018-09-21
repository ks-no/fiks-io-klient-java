package no.ks.fiks.svarinn.client.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilePayload implements Payload {
    private File payload;

    public FilePayload(File payload) {
        if (!payload.canRead())
            throw new RuntimeException(String.format("Filen \"%s\" er ikke lesbar, kan ikke konstruere file-payload for svarinn2", payload.getAbsolutePath()));
        this.payload = payload;
    }

    @Override
    public String getFilnavn() {
        return payload.getName();
    }

    @Override
    public InputStream getPayload() {
        try {
            return new FileInputStream(payload);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Kunne ikke finne filen \"%s\"", payload.getAbsolutePath()), e);
        }
    }
}
