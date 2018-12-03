package no.ks.fiks.svarinn2.klient.java;

import no.ks.fiks.componenttest.support.feign.TestApiBuilder;
import no.ks.fiks.componenttest.support.invoker.TestInvoker;
import no.ks.fiks.svarinn.client.SvarInnKlient;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.commons.MeldingsType;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKontoApi;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.AdresseSpesifikasjon;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KlientTest extends AutorisertServiceTest {

    KlientTest() {
        super(TestContext.FIKS_SVARINN2_SERVICE, TestContext.FIKS_SVARINN2_KATALOG_SERVICE);
    }

    @Test
    @DisplayName("Test at alice kan finne bobs konto, og sende han en melding")
    void testSendString(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);
        SvarInnKlient bobKlient = getBobKlient(generator);

        String payload = UUID.randomUUID().toString();
        aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload);

        CompletableFuture<MottattMelding> futureMelding = new CompletableFuture<>();

        bobKlient.newSubscription((m, k) -> futureMelding.complete(m));

        MottattMelding melding = futureMelding.get(10, TimeUnit.SECONDS);
        ZipInputStream dekryptertPayload = melding.getDekryptertPayload();

        assertEquals(payload, getPayloadAsString(dekryptertPayload, "payload.txt"));
    }
    private SvarInnKlient getAliceKlient(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        return generator.opprettKontoOgKlient(TestUtil.readP12(getClass().getResourceAsStream("/" + "alice-virksomhetssertifikat.p12"), "PASSWORD"), "PASSWORD", "et alias", "et alias", "PASSWORD");
    }

    private SvarInnKlient getBobKlient(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        return generator.opprettKontoOgKlient(TestUtil.readP12(getClass().getResourceAsStream("/" + "alice-virksomhetssertifikat.p12"), "PASSWORD"), "PASSWORD", "et alias", "et alias", "PASSWORD");
    }

    @Test
    @DisplayName("Test at alice kan finne bobs konto på lookup")
    void testLookup(@Autowired SvarInn2KlientGenerator generator, @Autowired TestApiBuilder<SvarInnKontoApi> kontoApi) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);
        SvarInnKlient bobKlient = getBobKlient(generator);

        String meldingType = UUID.randomUUID().toString();
        Identifikator identifikator = new Identifikator().identifikatorType(Identifikator.IdentifikatorTypeEnum.ORG_NO).identifikator("123456789");
        int sikkerhetsniva = 4;

        kontoApi.asPerson(TestUtil.randomPerson()).build().leggTilAdresse(bobKlient.getKontoId().getUuid(), new AdresseSpesifikasjon()
                .addSikkerhetsnivaerItem(sikkerhetsniva)
                .addIdentifikatorItem(identifikator)
                .addMeldingTypeItem(meldingType));

        Konto konto = new TestInvoker().invoke(() -> aliceKlient.lookup(LookupRequest.builder()
                .dokumentType(meldingType)
                .identifikator(identifikator)
                .sikkerhetsNiva(sikkerhetsniva)
                .build()).get());

        assertEquals(bobKlient.getKontoId(), konto.getKontoId());
    }

    @Test
    @DisplayName("Test at alice får en optional-empty for en lookup på en ikke-eksisterende adresse")
    void testEmptyLookup(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);

        Optional<Konto> konto = aliceKlient.lookup(LookupRequest.builder()
                .dokumentType(UUID.randomUUID().toString())
                .identifikator(TestUtil.randomOrgNoIdentifikator())
                .sikkerhetsNiva(4)
                .build());

        assertFalse(konto.isPresent());
    }

    @Test
    @DisplayName("Test at Bob kan kvittere akseptert på en melding fra Alice")
    void testKvitteringAkseptert(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);
        SvarInnKlient bobKlient = getBobKlient(generator);

        String payload = "heisann bob";
        SendtMelding sendtMelding = aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload);

        CompletableFuture<Melding> futureSendtKvittering = new CompletableFuture<>();
        bobKlient.newSubscription((m, k) -> futureSendtKvittering.complete(k.kvitterAkseptert()));

        CompletableFuture<Melding> futureMottattKvittering = new CompletableFuture<>();
        aliceKlient.newSubscription((m, k) -> futureMottattKvittering.complete(m));

        Melding sendtKvittering = futureSendtKvittering.get(10, TimeUnit.SECONDS);
        Melding mottattKvittering = futureMottattKvittering.get(10, TimeUnit.SECONDS);

        assertEquals(aliceKlient.getKontoId(), mottattKvittering.getMottakerKontoId());
        assertEquals(bobKlient.getKontoId(), mottattKvittering.getAvsenderKontoId());
        assertEquals(sendtKvittering.getMeldingId(), mottattKvittering.getMeldingId());
        assertEquals(sendtMelding.getMeldingId(), mottattKvittering.getSvarPaMelding());
        assertEquals(MeldingsType.KVITTERING_AKSEPTERT, mottattKvittering.getMeldingType());
    }

    @Test
    @DisplayName("Test at Bob kan kvittere avvist på en melding fra Alice")
    void testKvitteringAvvist(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);
        SvarInnKlient bobKlient = getBobKlient(generator);

        String payload = "heisann bob";
        SendtMelding sendtMelding = aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload);

        String kvitteringTekst = UUID.randomUUID().toString();
        CompletableFuture<Melding> futureSendtKvittering = new CompletableFuture<>();
        bobKlient.newSubscription((m, k) -> futureSendtKvittering.complete(k.kvitterAvvist(kvitteringTekst)));

        CompletableFuture<MottattMelding> futureMottattKvittering = new CompletableFuture<>();
        aliceKlient.newSubscription((m, k) -> futureMottattKvittering.complete(m));

        Melding sendtKvittering = futureSendtKvittering.get(10, TimeUnit.SECONDS);
        MottattMelding mottattKvittering = futureMottattKvittering.get(10, TimeUnit.SECONDS);

        assertEquals(aliceKlient.getKontoId(), mottattKvittering.getMottakerKontoId());
        assertEquals(bobKlient.getKontoId(), mottattKvittering.getAvsenderKontoId());
        assertEquals(sendtKvittering.getMeldingId(), mottattKvittering.getMeldingId());
        assertEquals(sendtMelding.getMeldingId(), mottattKvittering.getSvarPaMelding());
        assertEquals(MeldingsType.KVITTERING_AVVIST, mottattKvittering.getMeldingType());
        assertEquals(kvitteringTekst, getPayloadAsString(mottattKvittering.getDekryptertPayload(), "kvitteringTekst.txt"));
    }

    @Test
    @DisplayName("Test at Bob kan kvittere feilet på en melding fra Alice")
    void testKvitteringFeilet(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlient aliceKlient = getAliceKlient(generator);
        SvarInnKlient bobKlient = getBobKlient(generator);

        String payload = "heisann bob";
        SendtMelding sendtMelding = aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload);

        String kvitteringTekst = UUID.randomUUID().toString();
        CompletableFuture<Melding> futureSendtKvittering = new CompletableFuture<>();
        bobKlient.newSubscription( (m, k) -> futureSendtKvittering.complete(k.kvitterFeilet(kvitteringTekst)));

        CompletableFuture<MottattMelding> futureMottattKvittering = new CompletableFuture<>();
        aliceKlient.newSubscription((m, k) -> futureMottattKvittering.complete(m));

        Melding sendtKvittering = futureSendtKvittering.get(10, TimeUnit.SECONDS);
        MottattMelding mottattKvittering = futureMottattKvittering.get(10, TimeUnit.SECONDS);

        assertEquals(aliceKlient.getKontoId(), mottattKvittering.getMottakerKontoId());
        assertEquals(bobKlient.getKontoId(), mottattKvittering.getAvsenderKontoId());
        assertEquals(sendtKvittering.getMeldingId(), mottattKvittering.getMeldingId());
        assertEquals(sendtMelding.getMeldingId(), mottattKvittering.getSvarPaMelding());
        assertEquals(MeldingsType.KVITTERING_FEILET, mottattKvittering.getMeldingType());
        assertEquals(kvitteringTekst, getPayloadAsString(mottattKvittering.getDekryptertPayload(), "kvitteringTekst.txt"));
    }

    private String getPayloadAsString(ZipInputStream dekryptertPayload, String filename) throws IOException {
        ZipEntry entry;
        byte[] buffer = new byte[2048];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while((entry = dekryptertPayload.getNextEntry()) != null){
            if (entry.getName().equals(filename)){
                int len;
                while ((len = dekryptertPayload.read(buffer)) > 0)
                {
                    output.write(buffer, 0, len);
                }
            }
        }

        return new String(output.toByteArray());
    }

}
