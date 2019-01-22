package no.ks.fiks.svarinn.client.model;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("StringPayload")
class StringPayloadTest {

    @DisplayName("hente filnavn")
    @Test
    void getFilnavn() {
        final String filnavn = "file.json";
        final StringPayload payload = new StringPayload("payload", filnavn);
        assertEquals(filnavn, payload.getFilnavn());
    }

    @DisplayName("hente strøm")
    @Test
    void getPayload() throws IOException {
        final String content = "payload";
        final StringPayload payload = new StringPayload(content, "file.json");
        try (final InputStream payloadStream = payload.getPayload()) {
            final byte[] contentAsByte = new byte[content.length()];
            assertAll(() -> assertNotNull(payloadStream),
                      () -> ByteStreams.readFully(payloadStream, contentAsByte),
                      () -> assertEquals(content, new String(contentAsByte, StandardCharsets.UTF_8)));

        }
    }
}