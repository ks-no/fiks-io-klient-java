package no.ks.fiks.io.client;

import io.vavr.control.Option;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.client.model.*;
import no.ks.fiks.io.client.send.FiksIOSender;
import no.ks.fiks.io.klient.MeldingSpesifikasjonApiModel;
import no.ks.fiks.io.klient.SendtMeldingApiModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class FiksIOHandlerTest {

    @Mock
    private FiksIOSender utsendingKlient;

    @Mock
    private KatalogHandler katalogHandler;

    @Mock
    private AsicHandler asicHandler;

    @Mock
    private X509Certificate x509Certificate;

    private KontoId kontoId = new KontoId(UUID.randomUUID());

    private FiksIOHandler fiksIOHandler;

    @BeforeEach
    void setUp() {
        this.fiksIOHandler = new FiksIOHandler(kontoId, utsendingKlient, katalogHandler, asicHandler);
    }

    @Nested
    @DisplayName("Send")
    class Send {

        @DisplayName("tom payload")
        @Test
        void sendEmptyPayload() {
            final UUID mottakerKontoId = UUID.randomUUID();
            final MeldingRequest meldingRequest = MeldingRequest.builder()
                .meldingType("meldingsprotokoll")
                .mottakerKontoId(new KontoId(mottakerKontoId))
                .ttl(Duration.ofDays(5L))
                .build();
            final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
                .meldingId(UUID.randomUUID())
                .avsenderKontoId(UUID.randomUUID())
                .mottakerKontoId(mottakerKontoId)
                .meldingType("type")
                .ttl(meldingRequest.getTtl()
                    .toMillis())
                .build();
            when(utsendingKlient.send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class))).thenReturn(sendtMeldingApiModel);

            final SendtMelding sendtMelding = fiksIOHandler.send(meldingRequest, Collections.emptyList());
            assertAll(
                () -> assertEquals(meldingRequest.getMottakerKontoId()
                    .getUuid(), sendtMelding.getMottakerKontoId()
                    .getUuid()),
                () -> assertEquals(TimeUnit.DAYS.toMillis(5L), sendtMelding.getTtl()
                    .toMillis())
            );

            verify(utsendingKlient).send(isA(MeldingSpesifikasjonApiModel.class), eq(Option.none()));
            verifyNoMoreInteractions(utsendingKlient);
            verifyNoInteractions(katalogHandler, asicHandler, x509Certificate);
        }

        @DisplayName("med payload")
        @Test
        void harPayload() {
            final UUID mottakerKontoId = UUID.randomUUID();
            Duration ttl = Duration.ofDays(5L);
            final MeldingRequest meldingRequest = MeldingRequest.builder()
                .meldingType("meldingsprotokoll")
                .mottakerKontoId(new KontoId(mottakerKontoId))
                .ttl(ttl)
                .build();
            final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
                .meldingId(UUID.randomUUID())
                .avsenderKontoId(UUID.randomUUID())
                .meldingType(meldingRequest.getMeldingType())
                .mottakerKontoId(mottakerKontoId)
                .ttl(meldingRequest.getTtl()
                    .toMillis())
                .build();
            when(utsendingKlient.send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class))).thenReturn(sendtMeldingApiModel);
            when(katalogHandler.getPublicKey(eq(meldingRequest.getMottakerKontoId()))).thenReturn(x509Certificate);
            when(asicHandler.encrypt(same(x509Certificate), isA(List.class))).thenReturn(new ByteArrayInputStream(new byte[]{0, 1, 0, 1}));

            final SendtMelding sendtMelding = fiksIOHandler.send(meldingRequest,
                Collections.singletonList(new StringPayload("Test 1,2,3", "filnavn.txt")));
            assertAll(
                () -> assertEquals(meldingRequest.getMottakerKontoId()
                    .getUuid(), sendtMelding.getMottakerKontoId()
                    .getUuid()),
                () -> assertEquals(TimeUnit.DAYS.toMillis(5L), sendtMelding.getTtl()
                    .toMillis())
            );
            final ArgumentCaptor<MeldingSpesifikasjonApiModel> meldingRequestCaptor = ArgumentCaptor.forClass(MeldingSpesifikasjonApiModel.class);
            verify(utsendingKlient).send(meldingRequestCaptor.capture(), isA(Option.class));


            verify(katalogHandler).getPublicKey(eq(meldingRequest.getMottakerKontoId()));
            verify(asicHandler).encrypt(same(x509Certificate), isA(List.class));
            verifyNoMoreInteractions(utsendingKlient, asicHandler, x509Certificate, katalogHandler);

            MeldingSpesifikasjonApiModel meldingRequestCaptorValue = meldingRequestCaptor.getValue();
            assertEquals(ttl.toMillis(), meldingRequestCaptorValue.getTtl());
        }
    }

    @Nested
    @DisplayName("Bygg kvittering sender")
    class BuildKvitteringSender {
        @DisplayName("med normale instillinger")
        @Test
        void buildKvitteringSender() {

            final byte[] chunkOfData = {0, 1, 0, 1};
            final MottattMelding mottattMelding = MottattMelding.builder()
                .meldingId(new MeldingId(UUID.randomUUID()))
                .meldingType("MeldingType")
                .avsenderKontoId(new KontoId(UUID.randomUUID()))
                .mottakerKontoId(new KontoId(UUID.randomUUID()))
                .ttl(Duration.ofDays(3))
                .writeKryptertZip(path -> {
                })
                .getKryptertStream(() -> new ByteArrayInputStream(chunkOfData))
                .writeDekryptertZip(path -> {
                })
                .getDekryptertZipStream(
                    () -> new ZipInputStream(new ByteArrayInputStream(chunkOfData)))
                .build();

            final SvarSender svarSender = fiksIOHandler.buildKvitteringSender(AmqpChannelFeedbackHandler.builder().handleAck(() -> {
            }).build(), mottattMelding);
            assertNotNull(svarSender);

            verifyNoInteractions(utsendingKlient, katalogHandler, asicHandler, x509Certificate);
        }
    }

    @Nested
    @DisplayName("Send rådata")
    class SendRaw {

        @DisplayName("enkel ferdigpakke")
        @Test
        void sendRaw() throws IOException {
            final UUID mottakerKontoId = UUID.randomUUID();
            final String meldingsprotokoll = "meldingsprotokoll";
            final MeldingRequest meldingRequest = MeldingRequest.builder()
                .meldingType(meldingsprotokoll)
                .mottakerKontoId(new KontoId(mottakerKontoId))
                .ttl(Duration.ofDays(5L))
                .build();
            final SendtMeldingApiModel sendtMeldingApiModel = SendtMeldingApiModel.builder()
                .meldingId(UUID.randomUUID())
                .avsenderKontoId(UUID.randomUUID())
                .mottakerKontoId(mottakerKontoId)
                .meldingType(meldingsprotokoll)
                .ttl(meldingRequest.getTtl()
                    .toMillis())
                .build();
            when(utsendingKlient.send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class))).thenReturn(sendtMeldingApiModel);

            try (final InputStream dataStream = new ByteArrayInputStream("Innhold".getBytes())) {
                final SendtMelding sendtMelding = fiksIOHandler.sendRaw(meldingRequest, dataStream);
                assertNotNull(sendtMelding);
                assertEquals(sendtMelding.getMeldingId().getUuid(), sendtMeldingApiModel.getMeldingId());
            }
            verify(utsendingKlient).send(isA(MeldingSpesifikasjonApiModel.class), isA(Option.class));
            verifyNoInteractions(katalogHandler, asicHandler, x509Certificate);
        }
    }
}