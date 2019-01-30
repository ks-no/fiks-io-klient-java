package no.ks.fiks.svarinn.client;

import no.ks.fiks.svarinn.client.model.StreamPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 *
 */
@DisplayName("StreamPayload")
class StreamPayloadTest {

    @DisplayName("hent filnavn")
    @Test
    void getFilnavn() throws IOException {

        final String filNavn = "filnavn.json";
        try (final InputStream inputStream = new ByteArrayInputStream("content".getBytes())) {
            final StreamPayload streamPayload = new StreamPayload(inputStream, filNavn);
            assertEquals(filNavn, streamPayload.getFilnavn());
        }
    }

    @DisplayName("hente strøm")
    @Test
    void getPayload() throws IOException {
        try (final InputStream inputStream = new ByteArrayInputStream("content".getBytes())) {
            final StreamPayload streamPayload = new StreamPayload(inputStream, "filnavn.json");
            assertSame(inputStream, streamPayload.getPayload());
        }
    }
}