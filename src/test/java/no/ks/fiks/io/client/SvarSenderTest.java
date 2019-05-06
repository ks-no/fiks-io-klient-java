package no.ks.fiks.io.client;

import io.vavr.control.Option;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.client.model.SendtMelding;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("SvarSender")
@ExtendWith(MockitoExtension.class)
class SvarSenderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvarSenderTest.class);

    @Mock
    private FiksIOSender fiksIOSender;

    private AtomicBoolean ackCompleted;

    @BeforeEach
    void setUp() {
        ackCompleted = new AtomicBoolean(false);
    }

    @DisplayName("Sender svar")
    @Test
    void svar() throws IOException {

        final byte[] buf = {0, 1, 0, 1};
        try(final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);

            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            when(fiksIOSender.send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class)))
                .thenAnswer((Answer<SendtMeldingApiModel>) invocationOnMock -> {
                    MeldingSpesifikasjonApiModel meldingSpesifikasjonApiModel = invocationOnMock.getArgument(0);
                    return SendtMeldingApiModel.builder()
                                .avsenderKontoId(meldingSpesifikasjonApiModel.getAvsenderKontoId())
                                .meldingId(UUID.randomUUID())
                                .mottakerKontoId(meldingSpesifikasjonApiModel.getMottakerKontoId())
                                .ttl(Duration.ofHours(1L).toMillis())
                                .build();
                });
            final SendtMelding sendtMelding = svarSender.svar(mottattMelding.getMeldingType());
            assertNotNull(sendtMelding);
            assertFalse(ackCompleted.get());
            verify(fiksIOSender).send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class));
            verifyNoMoreInteractions(fiksIOSender);
        }
    }

    @DisplayName("Ack")
    @Test
    void ack() throws IOException {
        final byte[] buf = {0, 1, 0, 1};
        try(final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);
            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            svarSender.ack();
            assertTrue(ackCompleted.get());
            verifyZeroInteractions(fiksIOSender);
        }
    }

    private SvarSender createSvarSender(final byte[] buf, final MottattMelding mottattMelding) {
        return SvarSender.builder()
                         .doQueueAck(() -> {
                             LOGGER.info("ACK completed");
                             ackCompleted.set(true);
                         })
                         .encrypt(l -> new ByteArrayInputStream(buf))
                         .meldingSomSkalKvitteres(mottattMelding)
                         .utsendingKlient(fiksIOSender)
                         .build();
    }

    private MottattMelding createMottattMelding(final byte[] buf, final InputStream inputStream) {
        return MottattMelding.builder()
                             .meldingId(new MeldingId(UUID.randomUUID()))
                             .meldingType("meldingType")
                             .avsenderKontoId(new KontoId(UUID.randomUUID()))
                             .mottakerKontoId(new KontoId(UUID.randomUUID()))
                             .ttl(Duration.ofHours(1L))
                             .writeDekryptertZip(p -> LOGGER.info("Dekryptert '{}'", p))
                             .writeKryptertZip(p -> LOGGER.info("Kryptert '{}'", p))
                             .getKryptertStream(() -> new ByteArrayInputStream(buf))
                             .getDekryptertZipStream(() -> new ZipInputStream(inputStream))
                             .build();
    }
}