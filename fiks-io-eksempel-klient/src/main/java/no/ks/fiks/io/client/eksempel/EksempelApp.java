package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.eksempel.config.FiksIOKlientProperties;
import no.ks.fiks.io.client.eksempel.config.MaskinportenProperties;
import no.ks.fiks.io.client.eksempel.utils.JavaClientUtils;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class EksempelApp {
    private static final Logger logger = LoggerFactory.getLogger(EksempelApp.class);
    private static final FiksIOKlientProperties klientProperties = FiksIOKlientProperties.loadProperties("config.properties");
    private static final MaskinportenProperties maskinportenProperties = MaskinportenProperties.loadMaskinportenProperties("maskinporten.properties");

    public static void main(String[] args) throws Exception {
        final var kontoId = klientProperties.kontoId();

        try (var javaKlient = JavaClientUtils.lagJavaKlient(klientProperties, maskinportenProperties)) {
            javaKlient.newSubscription(new MeldingHandler()::behandleMelding);

            runInteractiveConsole(javaKlient, kontoId);
        }
    }

    private static void runInteractiveConsole(FiksIOKlient javaKlient, KontoId kontoId) {
        System.out.println("Starter interaktiv konsoll. Trykk på følgende taster:");
        System.out.println("  P - Send PING melding");
        System.out.println("  O - Send PONG melding");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim().toUpperCase();

                switch (input) {
                    case "P":
                        logger.info("P trykket. Sender PING melding til konto: {}", kontoId);
                        send(javaKlient, kontoId, "PING", "Dette er en PING melding");
                        break;
                    case "O":
                        logger.info("O trykket. Sender PONG melding til konto: {}", kontoId);
                        send(javaKlient, kontoId, "PONG", "Dette er en PONG melding");
                        break;
                    case "ESC":
                        logger.info("ESC trykket. Avslutter applikasjon");
                        return;
                    default:
                        if (!input.isEmpty()) {
                            logger.info("Ukjent kommando: {}. Prøv P, O eller ESC", input);
                        }
                }
            }
        }
    }

    public static void send(FiksIOKlient klient, KontoId kontoId, String meldingType, String innhold) {
        klient.send(MeldingRequest.builder().mottakerKontoId(kontoId).meldingType(meldingType).build(), innhold, "melding.txt");
    }
}


