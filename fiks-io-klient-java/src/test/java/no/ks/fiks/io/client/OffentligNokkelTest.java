package no.ks.fiks.io.client;

import com.google.common.io.Resources;
import feign.FeignException;
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;
import no.ks.fiks.fiksio.client.api.katalog.model.OffentligNokkel;
import no.ks.fiks.fiksio.client.api.konfigurasjon.api.FiksIoKontoApi;
import no.ks.fiks.fiksio.client.api.konfigurasjon.model.OppdaterOffentligNokkelSpesifikasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.model.KontoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Offentlig nøkkel-håndtering")
class OffentligNokkelTest {

    @Mock
    private FiksIoKatalogApi autentisertKatalogApi;

    @Mock
    private FiksIoKatalogApi publicKatalogApi;

    @Mock
    private FiksIoKontoApi kontoApi;

    @DisplayName("lastOppOffentligNokkel")
    @Nested
    class LastOppOffentligNokkel {

        @DisplayName("laster opp offentlig nøkkel når kontoApi er tilgjengelig")
        @Test
        void lasterOppNokkel() throws IOException {
            KontoId kontoId = new KontoId(UUID.randomUUID());
            String pem = lesOffentligNokkel();
            KatalogHandler handler = lagKatalogHandlerMedKontoApi();

            handler.uploadPublicKey(kontoId, pem);

            verify(kontoApi).settOffentligNokkel(eq(kontoId.getUuid()), any(OppdaterOffentligNokkelSpesifikasjon.class));
        }

        @DisplayName("kaster RuntimeException når kontoApi mangler")
        @Test
        void kasterExceptionNaarKontoApiMangler() throws IOException {
            KontoId kontoId = new KontoId(UUID.randomUUID());
            String pem = lesOffentligNokkel();
            KatalogHandler handler = lagKatalogHandlerUtenKontoApi();

            RuntimeException exception = assertThrows(RuntimeException.class, () -> handler.uploadPublicKey(kontoId, pem));
            assertEquals("Kan ikke laste opp offentlig nøkkel grunnet manglene FiksIOKontoApi klient", exception.getMessage());
        }

        @DisplayName("getPublicKey returnerer null når katalogApi kaster FeignException.NotFound")
        @Test
        void getPublicKeyReturnerNullNaarKatalogApiKasterNotFound() {
            KontoId kontoId = new KontoId(UUID.randomUUID());
            when(publicKatalogApi.getOffentligNokkel(kontoId.getUuid()))
                .thenThrow(FeignException.NotFound.class);

            KatalogHandler handler = lagKatalogHandlerMedKontoApi();
            assertNull(handler.getPublicKey(kontoId));
        }

        @DisplayName("getPublicKey returnerer null når offentlig nøkkel ikke finnes i katalogen")
        @Test
        void getPublicKeyReturnerNullNaarNokkelIkkeFinnes() {
            KontoId kontoId = new KontoId(UUID.randomUUID());
            when(publicKatalogApi.getOffentligNokkel(kontoId.getUuid()))
                .thenReturn(new OffentligNokkel());

            KatalogHandler handler = lagKatalogHandlerMedKontoApi();
            assertThrows(RuntimeException.class, () -> handler.getPublicKey(kontoId));
        }

        @DisplayName("getPublicKey kaster RuntimeException ved ugyldig sertifikat (CertificateException)")
        @Test
        void getPublicKeyKasterRuntimeExceptionVedUgyldigSertifikat() {
            KontoId kontoId = new KontoId(UUID.randomUUID());
            when(publicKatalogApi.getOffentligNokkel(kontoId.getUuid()))
                .thenReturn(new OffentligNokkel().nokkel("dette-er-ikke-et-gyldig-sertifikat"));

            KatalogHandler handler = lagKatalogHandlerMedKontoApi();
            RuntimeException exception = assertThrows(RuntimeException.class, () -> handler.getPublicKey(kontoId));
            assertTrue(exception.getMessage().contains(kontoId.toString()));
        }
    }

    @DisplayName("validerOffentligNokkelMotPrivateKey")
    @Nested
    class ValiderOffentligNokkel {

        @DisplayName("returnerer true når offentlig nøkkel matcher privat nøkkel")
        @Test
        void returnerTrueNaarNokkelMatcherPrivatNokkel() throws IOException {
            String pem = lesOffentligNokkel();
            KontoKonfigurasjon konfigurasjon = KontoKonfigurasjon.builder()
                .kontoId(new KontoId(UUID.randomUUID()))
                .privatNokkel(TestUtil.generatePrivateKey())
                .build();

            KeyValidatorHandler validator = new KeyValidatorHandler(lagKatalogHandlerMedKontoApi(), konfigurasjon);
            assertTrue(validator.validerOffentligNokkelMotPrivateKey(pem));
        }

        @DisplayName("returnerer false når offentlig nøkkel ikke matcher privat nøkkel")
        @Test
        void returnerFalseNaarNokkelIkkeMatcherPrivatNokkel() throws IOException {
            String pem = lesOffentligNokkel();
            KontoKonfigurasjon konfigurasjon = KontoKonfigurasjon.builder()
                .kontoId(new KontoId(UUID.randomUUID()))
                .privatNokkel(TestUtil.generateDifferentPrivateKey()) // bob sin nøkkel
                .build();

            KeyValidatorHandler validator = new KeyValidatorHandler(lagKatalogHandlerMedKontoApi(), konfigurasjon);
            assertFalse(validator.validerOffentligNokkelMotPrivateKey(pem));
        }

        @DisplayName("kaster IllegalStateException når ingen private nøkler er konfigurert")
        @Test
        void kasterIllegalStateExceptionNaarIngenPrivateNokler() throws IOException {
            String pem = lesOffentligNokkel();
            KontoId kontoId = new KontoId(UUID.randomUUID());
            KontoKonfigurasjon konfigurasjon = KontoKonfigurasjon.builder()
                .kontoId(kontoId)
                .privateNokler(List.of())
                .build();

            KeyValidatorHandler validator = new KeyValidatorHandler(lagKatalogHandlerMedKontoApi(), konfigurasjon);
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validator.validerOffentligNokkelMotPrivateKey(pem));
            assertEquals(
                "Ingen private nøkler er satt opp i kontokonfigurasjon, kan ikke validere offentlig nøkkel for konto " + kontoId,
                exception.getMessage()
            );
        }

        @DisplayName("returnerer false ved ugyldig PEM-streng")
        @Test
        void returnerFalseNaarPemErUgyldig() {
            KontoKonfigurasjon konfigurasjon = KontoKonfigurasjon.builder()
                .kontoId(new KontoId(UUID.randomUUID()))
                .privatNokkel(TestUtil.generatePrivateKey())
                .build();

            KeyValidatorHandler validator = new KeyValidatorHandler(lagKatalogHandlerMedKontoApi(), konfigurasjon);
            assertFalse(validator.validerOffentligNokkelMotPrivateKey("dette-er-ikke-en-gyldig-pem"));
        }
    }

    private KatalogHandler lagKatalogHandlerMedKontoApi() {
        return new KatalogHandler(autentisertKatalogApi, publicKatalogApi, kontoApi);
    }

    private KatalogHandler lagKatalogHandlerUtenKontoApi() {
        return new KatalogHandler(autentisertKatalogApi, publicKatalogApi, null);
    }

    private String lesOffentligNokkel() throws IOException {
        return new String(Resources.toByteArray(getClass().getResource("/alice.cert")), StandardCharsets.UTF_8);
    }
}
