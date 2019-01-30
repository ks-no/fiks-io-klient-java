package no.ks.fiks.svarinn.client.model;

import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static java.nio.file.Files.*;

public class FilePayload implements Payload {
    private Path payloadPath;

    public FilePayload(@NonNull Path path) {
        if (!isRegularFile(path))
            throw new RuntimeException(String.format("Path \"%s\" representerer ikke en fil, kan ikke konstruere file-payload for svarinn2", path.toString()));
        if (!isReadable(path))
            throw new RuntimeException(String.format("Filen \"%s\" er ikke lesbar, kan ikke konstruere file-payload for svarinn2", path.toString()));

        this.payloadPath = path;
    }

    @Override
    public String getFilnavn() {
        return payloadPath.getFileName().toString();
    }

    @Override
    public InputStream getPayload() {
        try {
            return newInputStream(payloadPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
