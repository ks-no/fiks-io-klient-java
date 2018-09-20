package no.ks.fiks.svarinn2.klient.java;

import com.rabbitmq.client.ConnectionFactory;
import no.ks.fiks.componenttest.support.spring.ServiceComponentTest;
import no.ks.fiks.svarinn.client.SubscribeSettings;
import no.ks.fiks.svarinn.client.SvarInn2;
import no.ks.fiks.svarinn.client.model.MeldingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KlientTest extends ServiceComponentTest {

    public KlientTest() {
        super(TestContext.FIKS_SVARINN2_SERVICE, TestContext.FIKS_SVARINN2_KATALOG_SERVICE);
    }

    @Test
    void name(@Autowired SvarInn2KlientGenerator generator) throws Exception {
        ConnectionFactory aliceConnection = new ConnectionFactory();
        SvarInn2 aliceKlient = generator.opprettKontoOgKlient(getClass().getResourceAsStream("/" + "kommune1.p12"), aliceConnection);

        ConnectionFactory bobConnection = new ConnectionFactory();
        SvarInn2 bobKlient = generator.opprettKontoOgKlient(getClass().getResourceAsStream("/" + "kommune1.p12"), bobConnection);

        String payload = "heisann bob";
        aliceKlient.send(MeldingRequest.builder()
                .meldingType("no.ks.fiks.digisos")
                .mottakerKontoId(bobKlient.getKontoId())
                .build(), payload);

        CompletableFuture<String> melding = new CompletableFuture<>();
        bobKlient.subscribe(bobConnection.newConnection().createChannel(), SubscribeSettings.builder()
                        .onMelding((m, k) -> {
                            System.out.println("Mottok melding " + m.getMeldingId());
                            k.kvitterAkseptert();
                            melding.complete(new String(m.getDekryptertPayload()));
                        })
                        .onClose(error -> System.exit(1))
                        .build());

        assertEquals(payload, melding.get(10, TimeUnit.SECONDS));
    }

}
