package no.ks.fiks.io.client;

import com.google.common.io.Resources;
import feign.Request;
import feign.codec.DecodeException;
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;
import no.ks.fiks.fiksio.client.api.katalog.model.OffentligNokkel;
import no.ks.fiks.io.client.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KatalogHandlerTest {

    @Mock
    private FiksIoKatalogApi fiksIoKatalogApi;

    @InjectMocks
    private KatalogHandler katalogHandler;

    @DisplayName("Hent offentlig nøkkel")
    @Nested
    class GetPublicKey {
        @DisplayName("feiler med exception")
        @Test
        void getPublicKeyFails() {
            byte[] body = null;
            final Request request = Request.create(Request.HttpMethod.GET, "/fiks-io/katalog/api/v1/kontoer/{kontoId}/offentligNokkel", Collections.emptyMap(), body, StandardCharsets.UTF_8, null);
            when(fiksIoKatalogApi.getOffentligNokkel(isA(UUID.class))).thenThrow(new DecodeException(400, "Could not decode", request));
            final UUID kontoId = UUID.randomUUID();
            assertThrows(DecodeException.class, () -> katalogHandler.getPublicKey(new KontoId(kontoId)));
            verify(fiksIoKatalogApi).getOffentligNokkel(eq(kontoId));
            verifyNoMoreInteractions(fiksIoKatalogApi);
        }

        @DisplayName("feiler under lesing av nøkkel")
        @Test
        void getPublicKeyFoundButFails() {
            when(fiksIoKatalogApi.getOffentligNokkel(isA(UUID.class))).thenReturn(new OffentligNokkel().nokkel("something")
                                                                                                        .serial("0x523DC4FE")
                                                                                                        .issuerDN("CN=KS,OU=Alice,O=KS - 971032146,L=HAAKON VIISGT 9 0161 OSLO,C=NO")
                                                                                                        .subjectDN("CN=KS,OU=Alice,O=KS - 971032146,L=HAAKON VIISGT 9 0161 OSLO,C=NO")
                                                                                                        .validFrom(OffsetDateTime.now()
                                                                                                                                 .minusYears(1L))
                                                                                                        .validTo(OffsetDateTime.now()
                                                                                                                               .plusYears(1L))
            );
            final UUID kontoId = UUID.randomUUID();
            assertThrows(RuntimeException.class, () -> katalogHandler.getPublicKey(new KontoId(kontoId)));
            verify(fiksIoKatalogApi).getOffentligNokkel(eq(kontoId));
            verifyNoMoreInteractions(fiksIoKatalogApi);
        }

        @Test
        void getPublicKeyFoundAndValid() throws IOException {

            final byte[] certificateChunk = Resources.toByteArray(getClass().getResource("/alice-virksomhetssertifikat.crt"));


            when(fiksIoKatalogApi.getOffentligNokkel(isA(UUID.class))).thenReturn(new OffentligNokkel().nokkel(new String(certificateChunk,
                                                                                                                           StandardCharsets.UTF_8))
                                                                                                        .serial("0x523DC4FE")
                                                                                                        .issuerDN("CN=KS,OU=Alice,O=KS - 971032146,L=HAAKON VIISGT 9 0161 OSLO,C=NO")
                                                                                                        .subjectDN("CN=KS,OU=Alice,O=KS - 971032146,L=HAAKON VIISGT 9 0161 OSLO,C=NO")
                                                                                                        .validFrom(OffsetDateTime.now()
                                                                                                                                 .minusYears(1L))
                                                                                                        .validTo(OffsetDateTime.now()
                                                                                                                               .plusYears(1L))
            );
            final UUID kontoId = UUID.randomUUID();
            assertNotNull(katalogHandler.getPublicKey(new KontoId(kontoId)));
        }
    }

}