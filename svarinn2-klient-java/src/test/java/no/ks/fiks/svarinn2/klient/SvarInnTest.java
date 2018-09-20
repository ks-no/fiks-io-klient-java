/*
package no.ks.fiks.svarinn2.klient;

import com.rabbitmq.client.ConnectionFactory;
import no.ks.fiks.svarinn.client.*;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.SendtMelding;
import no.ks.fiks.svarinn.client.model.MeldingRequest;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.svarinn2.swagger.invoker.v1.ApiClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator.IdentifikatorTypeEnum.NAV_ENHET_ID;
import static no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator.IdentifikatorTypeEnum.ORG_NO;

class SvarInnTest {

    @Test
    void name() throws IOException, TimeoutException {
        //konfigurer svarInn2 klienten
        SvarInn2 svarInn2 = new SvarInn2(SvarInnKonfigurasjon.builder()
                .kontoId(new KontoId(UUID.randomUUID()))
                .lookupCache(p -> new KontoId(UUID.randomUUID()))
                .svarInn2Api(new ApiClient().buildClient(SvarInnApi.class))
                .fiksIntegrasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(UUID.randomUUID())
                        .integrasjonPassord("et passord")
                        .virksomhetsertifikat("et virkesomhetssertifikat")
                        .build())
                .build());

        //finn en konto basert på organisajonsnummer
        Optional<Konto> konto = svarInn2.lookup(LookupRequest.builder()
                .identifikator(new Identifikator().identifikator("123456789").identifikatorType(ORG_NO))
                .dokumentType("digisos.v1")
                .sikkerhetsNiva(4)
                .build());

        //eller finn en konto basert på nav-kontor id
        Optional<Konto> navKontorKontoId = svarInn2.lookup(LookupRequest.builder()
                .identifikator(new Identifikator().identifikator("123456789").identifikatorType(NAV_ENHET_ID))
                .dokumentType("digisos.v1")
                .sikkerhetsNiva(4)
                .build());

        //send en melding
        SendtMelding melding = svarInn2.send(MeldingRequest.builder()
                .mottakerKontoId(konto.get().getKontoId())
                .ttl(Duration.ofDays(1))
                .build(),
                new ByteArrayInputStream(new byte[0]));

        //svar på en mottatt melding
        svarInn2.send(MeldingRequest.builder()
                .mottakerKontoId(konto.get().getKontoId())
                .ttl(Duration.ofDays(1))
                .svarPaMelding(melding.getMeldingId())
                .build(), new ByteArrayInputStream(new byte[0]));

        //subscribe på innkommende meldinger, og kvitter OK til avsender ved mottak
        svarInn2.subscribe(new ConnectionFactory().newConnection().createChannel(), SubscribeSettings.builder()
                .onMelding((m, k) -> {
                    System.out.println("Mottok melding " + m.getMeldingId());
                    k.kvitterAkseptert();
                })
                .onClose(error -> System.exit(1))
                .build());
    }
}*/
