package no.ks.fiks.svarinn2.klient.java;

import com.google.common.base.Stopwatch;
import no.ks.fiks.componenttest.support.feign.TestApiBuilder;
import no.ks.fiks.componenttest.support.invoker.TestInvoker;
import no.ks.fiks.svarinn.client.SvarInnKlientImpl;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.commons.MeldingsType;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKontoApi;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class KlientTest extends AutorisertServiceTest {

    KlientTest() {
        super(TestContext.FIKS_SVARINN2_SERVICE, TestContext.FIKS_SVARINN2_KATALOG_SERVICE);
    }

    @Test
    @DisplayName("Test at alice kan sende bob en melding som string")
    void testSendString(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        String payload = UUID.randomUUID().toString();

        Stopwatch started = Stopwatch.createUnstarted();
        CompletableFuture<MottattMelding> futureMelding = new CompletableFuture<>();

        bobKlient.newSubscription((m, k) -> {
            futureMelding.complete(m);
            System.out.println("motatt: " + started.elapsed().toMillis());
        });

        started.start();
        aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload, "payload.txt");


        MottattMelding melding = futureMelding.get(10, TimeUnit.SECONDS);
        ZipInputStream dekryptertPayload = melding.getDekryptertZipStream();

        assertEquals(payload, getPayloadAsString(dekryptertPayload, "payload.txt"));
    }

    @Test
    @DisplayName("Test at alice kan sende bob en melding som fil")
    void testSendFil(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        Path file = Paths.get("src/test/resources/small.pdf");
        aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), file);

        CompletableFuture<MottattMelding> futureMelding = new CompletableFuture<>();

        bobKlient.newSubscription((m, k) -> futureMelding.complete(m));

        MottattMelding melding = futureMelding.get(10, TimeUnit.SECONDS);
        ZipInputStream dekryptertPayload = melding.getDekryptertZipStream();

        assertEquals(file.toFile().length(), getPayload(dekryptertPayload, "small.pdf").length);
    }

    @Test
    @DisplayName("Test at alice kan sende bob en melding som stream")
    void testSendStream(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        byte[] bytes = TestUtil.randomBytes();
        aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), new ByteArrayInputStream(bytes), "payload.txt");

        CompletableFuture<MottattMelding> futureMelding = new CompletableFuture<>();

        bobKlient.newSubscription((m, k) -> futureMelding.complete(m));

        MottattMelding melding = futureMelding.get(10, TimeUnit.SECONDS);
        ZipInputStream dekryptertPayload = melding.getDekryptertZipStream();

        assertArrayEquals(bytes, getPayload(dekryptertPayload, "payload.txt"));
    }

    @Test
    @DisplayName("Test at alice kan finne bobs konto på lookup")
    void testLookup(@Autowired SvarInn2KlientGenerator generator, @Autowired TestApiBuilder<SvarInnKontoApi> kontoApi) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        String meldingType = UUID.randomUUID().toString();
        Identifikator identifikator = new Identifikator().identifikatorType(Identifikator.IdentifikatorTypeEnum.ORG_NO).identifikator("123456789");
        int sikkerhetsniva = 4;

        SvarInnKontoApi svarInnKontoApi = kontoApi.asPerson(TestUtil.randomPerson()).build();

        Adresse adresse = svarInnKontoApi.opprettAdresse(bobKlient.getKontoId().getUuid(), new AdresseSpesifikasjon().beskrivelse(UUID.randomUUID().toString()));
        svarInnKontoApi.leggTilIdentifikator(bobKlient.getKontoId().getUuid(), adresse.getAdresseId(), singletonList(identifikator));
        svarInnKontoApi.leggTilSikkerhetsnivaer(bobKlient.getKontoId().getUuid(), adresse.getAdresseId(), singletonList(new SikkerhetsnivaSpesifikasjon().sikkerhetsniva(sikkerhetsniva)));
        svarInnKontoApi.leggTilMeldingstyper(bobKlient.getKontoId().getUuid(), adresse.getAdresseId(), singletonList(new MeldingstypeSpesifikasjon().meldingstype(meldingType)));

        svarInnKontoApi.aktiverAdresse(bobKlient.getKontoId().getUuid(), adresse.getAdresseId());

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
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);

        Optional<Konto> konto = aliceKlient.lookup(LookupRequest.builder()
                .dokumentType(UUID.randomUUID().toString())
                .identifikator(TestUtil.randomOrgNoIdentifikator())
                .sikkerhetsNiva(4)
                .build());

        assertFalse(konto.isPresent());
    }

    @Test
    @DisplayName("Test at Bob kan svare Alice med en melding uten body")
    void testKvitteringAkseptert(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        String payload = "heisann bob";
        SendtMelding sendtMelding = aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload, "payload.txt");

        CompletableFuture<Melding> futureSendtKvittering = new CompletableFuture<>();
        bobKlient.newSubscription((m, k) -> futureSendtKvittering.complete(k.svar(MeldingsType.KVITTERING_AKSEPTERT)));

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
    @DisplayName("Test at Bob kan svare Alice med en melding med body")
    void testKvitteringAvvist(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        SvarInnKlientImpl aliceKlient = getAliceKlient(generator);
        SvarInnKlientImpl bobKlient = getBobKlient(generator);

        String payload = "heisann bob";
        SendtMelding sendtMelding = aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload, "payload.txt");

        String kvitteringTekst = UUID.randomUUID().toString();
        CompletableFuture<Melding> futureSendtKvittering = new CompletableFuture<>();
        bobKlient.newSubscription((m, k) -> futureSendtKvittering.complete(k.svar(MeldingsType.KVITTERING_AVVIST, kvitteringTekst, "payload.txt")));

        CompletableFuture<MottattMelding> futureMottattKvittering = new CompletableFuture<>();
        aliceKlient.newSubscription((m, k) -> futureMottattKvittering.complete(m));

        Melding sendtKvittering = futureSendtKvittering.get(10, TimeUnit.SECONDS);
        MottattMelding mottattKvittering = futureMottattKvittering.get(10, TimeUnit.SECONDS);

        assertEquals(aliceKlient.getKontoId(), mottattKvittering.getMottakerKontoId());
        assertEquals(bobKlient.getKontoId(), mottattKvittering.getAvsenderKontoId());
        assertEquals(sendtKvittering.getMeldingId(), mottattKvittering.getMeldingId());
        assertEquals(sendtMelding.getMeldingId(), mottattKvittering.getSvarPaMelding());
        assertEquals(MeldingsType.KVITTERING_AVVIST, mottattKvittering.getMeldingType());
        assertEquals(kvitteringTekst, getPayloadAsString(mottattKvittering.getDekryptertZipStream(), "payload.txt"));
    }

    private byte[] getPayload(ZipInputStream dekryptertPayload, String filename) throws IOException {
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
        return output.toByteArray();
    }

    private String getPayloadAsString(ZipInputStream dekryptertPayload, String filename) throws IOException {
        return new String(getPayload(dekryptertPayload, filename));
    }

    private SvarInnKlientImpl getAliceKlient(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        return generator.opprettKontoOgKlient(TestUtil.readP12(getClass().getResourceAsStream("/" + "alice-virksomhetssertifikat.p12"), "PASSWORD"), "PASSWORD", "et alias", "et alias", "PASSWORD");
    }

    private SvarInnKlientImpl getBobKlient(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        return generator.opprettKontoOgKlient(TestUtil.readP12(getClass().getResourceAsStream("/" + "alice-virksomhetssertifikat.p12"), "PASSWORD"), "PASSWORD", "et alias", "et alias", "PASSWORD");
    }
}
