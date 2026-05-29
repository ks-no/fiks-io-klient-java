package no.ks.fiks.io.client;

import no.ks.fiks.io.client.konfigurasjon.AsymmetriskNokkelKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.IdPortenKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.maskinporten.Maskinportenklient;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiksIOKlientFactoryTest {

    private static KontoKonfigurasjon kontoKonfigurasjon() {
        return KontoKonfigurasjon.builder()
            .kontoId(new KontoId(UUID.randomUUID()))
            .privateNokler(Arrays.asList(TestUtil.generatePrivateKey()))
            .build();
    }

    private static VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon() {
        return VirksomhetssertifikatKonfigurasjon.builder()
            .keyStorePassword("PASSWORD")
            .keyStore(TestUtil.readAliceVirksomhetssertifikat())
            .keyAlias("et alias")
            .keyPassword("PASSWORD")
            .build();
    }

    private static FiksIntegrasjonKonfigurasjon fiksIntegrasjonKonfigurasjon(IdPortenKonfigurasjon idPortenKonfigurasjon) {
        return FiksIntegrasjonKonfigurasjon.builder()
            .integrasjonId(UUID.randomUUID())
            .integrasjonPassord(UUID.randomUUID().toString())
            .idPortenKonfigurasjon(idPortenKonfigurasjon)
            .build();
    }

    @Test
    void build() {
        final IdPortenKonfigurasjon idPortenKonfigurasjon = IdPortenKonfigurasjon.builder()
            .accessTokenUri("http://localhost")
            .idPortenAudience("audience")
            .klientId(UUID.randomUUID().toString())
            .build();
        final FiksApiKonfigurasjon fiksApiKonfigurasjon = FiksApiKonfigurasjon.builder()
            .host("localhost")
            .port(9190)
            .build();
        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
            .kontoKonfigurasjon(kontoKonfigurasjon())
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon())
            .fiksIntegrasjonKonfigurasjon(fiksIntegrasjonKonfigurasjon(idPortenKonfigurasjon))
            .fiksApiKonfigurasjon(fiksApiKonfigurasjon)
            .build();
        assertThrows(RuntimeException.class, () -> new FiksIOKlientFactory(fiksIOKonfigurasjon).build());
    }

    @Test
    void getMaskinportenKlientBrukerVirksomhetssertifikatNarKeyIdentifierIkkeErSatt() {
        final IdPortenKonfigurasjon idPortenKonfigurasjon = IdPortenKonfigurasjon.builder()
            .accessTokenUri("http://localhost")
            .idPortenAudience("audience")
            .klientId(UUID.randomUUID().toString())
            .build();
        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
            .kontoKonfigurasjon(kontoKonfigurasjon())
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon())
            .fiksIntegrasjonKonfigurasjon(fiksIntegrasjonKonfigurasjon(idPortenKonfigurasjon))
            .build();

        final Maskinportenklient maskinportenklient = FiksIOKlientFactory.getMaskinportenKlient(fiksIOKonfigurasjon);
        assertNotNull(maskinportenklient);
    }

    @Test
    void getMaskinportenKlientBrukerAsymmetriskNokkelNarKeyIdentifierErSatt() {
        final IdPortenKonfigurasjon idPortenKonfigurasjon = IdPortenKonfigurasjon.builder()
            .accessTokenUri("http://localhost")
            .idPortenAudience("audience")
            .klientId(UUID.randomUUID().toString())
            .keyIdentifier("min-key-id")
            .build();
        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
            .kontoKonfigurasjon(kontoKonfigurasjon())
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon())
            .fiksIntegrasjonKonfigurasjon(fiksIntegrasjonKonfigurasjon(idPortenKonfigurasjon))
            .asymmetriskNokkelKonfigurasjon(AsymmetriskNokkelKonfigurasjon.builder()
                .privatNokkel(TestUtil.generatePrivateKey())
                .build())
            .build();

        final Maskinportenklient maskinportenklient = FiksIOKlientFactory.getMaskinportenKlient(fiksIOKonfigurasjon);
        assertNotNull(maskinportenklient);
    }

    @Test
    void getMaskinportenKlientFeilerNarKeyIdentifierErSattUtenAsymmetriskNokkelKonfigurasjon() {
        final IdPortenKonfigurasjon idPortenKonfigurasjon = IdPortenKonfigurasjon.builder()
            .accessTokenUri("http://localhost")
            .idPortenAudience("audience")
            .klientId(UUID.randomUUID().toString())
            .keyIdentifier("min-key-id")
            .build();
        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
            .kontoKonfigurasjon(kontoKonfigurasjon())
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon())
            .fiksIntegrasjonKonfigurasjon(fiksIntegrasjonKonfigurasjon(idPortenKonfigurasjon))
            .build();

        assertThrows(RuntimeException.class, () -> FiksIOKlientFactory.getMaskinportenKlient(fiksIOKonfigurasjon));
    }

}
