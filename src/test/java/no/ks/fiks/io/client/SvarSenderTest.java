package no.ks.fiks.io.client;

import com.google.common.collect.ImmutableMap;
import no.ks.fiks.io.client.model.*;
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
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@DisplayName("SvarSender")
@ExtendWith(MockitoExtension.class)
class SvarSenderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvarSenderTest.class);
    public static final String MELDING_TYPE = "meldingType";

    @Mock
    private FiksIOSender fiksIOSender;

    private AtomicBoolean ackCompleted;
    private AtomicBoolean nacked;
    private AtomicBoolean nackedWithRequeue;

    @BeforeEach
    void setUp() {
        ackCompleted = new AtomicBoolean(false);
        nacked = new AtomicBoolean(false);
        nackedWithRequeue = new AtomicBoolean(false);
    }

    @DisplayName("Sender svar")
    @Test
    void svar() throws IOException {

        final byte[] buf = {0, 1, 0, 1};
        try (final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);

            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            when(fiksIOSender.send(isA(MeldingSpesifikasjonApiModel.class), isA(Optional.class)))
                .thenAnswer((Answer<SendtMeldingApiModel>) invocationOnMock -> {
                    MeldingSpesifikasjonApiModel meldingSpesifikasjonApiModel = invocationOnMock.getArgument(0);
                    return SendtMeldingApiModel.builder()
                        .avsenderKontoId(meldingSpesifikasjonApiModel.getAvsenderKontoId())
                        .meldingId(UUID.randomUUID())
                        .mottakerKontoId(meldingSpesifikasjonApiModel.getMottakerKontoId())
                        .ttl(Duration.ofHours(1L).toMillis())
                        .meldingType(MELDING_TYPE)
                        .headere(Collections.emptyMap())
                        .build();
                });
            final SendtMelding sendtMelding = svarSender.svar(mottattMelding.getMeldingType());
            assertNotNull(sendtMelding);
            assertFalse(ackCompleted.get());
            verify(fiksIOSender).send(isA(MeldingSpesifikasjonApiModel.class), isA(Optional.class));
            verifyNoMoreInteractions(fiksIOSender);
        }
    }

    @DisplayName("Sender svar med klientMeldingId")
    @Test
    void svarWithKlientmeldingId() throws IOException {

        final byte[] buf = {0, 1, 0, 1};
        try (final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);
            final MeldingId klientMeldingId = new MeldingId(UUID.randomUUID());
            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            when(fiksIOSender.send(isA(MeldingSpesifikasjonApiModel.class), isA(Optional.class)))
                .thenAnswer((Answer<SendtMeldingApiModel>) invocationOnMock -> {
                    MeldingSpesifikasjonApiModel meldingSpesifikasjonApiModel = invocationOnMock.getArgument(0);
                    return SendtMeldingApiModel.builder()
                        .avsenderKontoId(meldingSpesifikasjonApiModel.getAvsenderKontoId())
                        .meldingId(UUID.randomUUID())
                        .mottakerKontoId(meldingSpesifikasjonApiModel.getMottakerKontoId())
                        .ttl(Duration.ofHours(1L).toMillis())
                        .meldingType(MELDING_TYPE)
                        .headere(ImmutableMap.of(Melding.HeaderKlientMeldingId, meldingSpesifikasjonApiModel.getHeadere().get(Melding.HeaderKlientMeldingId)))
                        .build();
                });

            final SendtMelding sendtMelding = svarSender.svar(mottattMelding.getMeldingType(), klientMeldingId);
            assertEquals(klientMeldingId, sendtMelding.getKlientMeldingId());
            assertNotNull(sendtMelding);
            assertFalse(ackCompleted.get());
            verify(fiksIOSender).send(isA(MeldingSpesifikasjonApiModel.class), isA(Optional.class));
            verifyNoMoreInteractions(fiksIOSender);
        }
    }

    @DisplayName("Ack")
    @Test
    void ack() throws IOException {
        final byte[] buf = {0, 1, 0, 1};
        try (final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);
            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            svarSender.ack();
            assertTrue(ackCompleted.get());
            verifyNoInteractions(fiksIOSender);
        }
    }

    @DisplayName("Nack")
    @Test
    void nack() throws IOException {
        final byte[] buf = {0, 1, 0, 1};
        try (final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);
            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            svarSender.nack();
            assertTrue(nacked.get());
            verifyNoInteractions(fiksIOSender);
        }
    }

    @DisplayName("Nack with requeue")
    @Test
    void nackWithRequeue() throws IOException {
        final byte[] buf = {0, 1, 0, 1};
        try (final InputStream inputStream = new ByteArrayInputStream(buf)) {
            final MottattMelding mottattMelding = createMottattMelding(buf, inputStream);
            final SvarSender svarSender = createSvarSender(buf, mottattMelding);
            svarSender.nackWithRequeue();
            assertTrue(nackedWithRequeue.get());
            verifyNoInteractions(fiksIOSender);
        }
    }



    private SvarSender createSvarSender(final byte[] buf, final MottattMelding mottattMelding) {
        return SvarSender.builder()
            .amqpChannelFeedbackHandler(AmqpChannelFeedbackHandler.builder()
                .handleAck(() -> {
                    LOGGER.info("ACK completed");
                    ackCompleted.set(true);
                })
                .handleNack(() -> nacked.set(true))
                .handNackWithRequeue(() -> nackedWithRequeue.set(true))
                .build())
            .encrypt(l -> new ByteArrayInputStream(buf))
            .meldingSomSkalKvitteres(mottattMelding)
            .utsendingKlient(fiksIOSender)
            .build();
    }

    private MottattMelding createMottattMelding(final byte[] buf, final InputStream inputStream) {
        return MottattMelding.builder()
            .meldingId(new MeldingId(UUID.randomUUID()))
            .meldingType(MELDING_TYPE)
            .avsenderKontoId(new KontoId(UUID.randomUUID()))
            .mottakerKontoId(new KontoId(UUID.randomUUID()))
            .ttl(Duration.ofHours(1L))
            .headere(Collections.emptyMap())
            .writeDekryptertZip(p -> LOGGER.info("Dekryptert '{}'", p))
            .writeKryptertZip(p -> LOGGER.info("Kryptert '{}'", p))
            .getKryptertStream(() -> new ByteArrayInputStream(buf))
            .getDekryptertZipStream(() -> new ZipInputStream(inputStream))
            .build();
    }
}