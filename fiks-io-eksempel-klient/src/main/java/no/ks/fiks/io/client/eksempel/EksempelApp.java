package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.eksempel.config.AmqpProperties;
import no.ks.fiks.io.client.eksempel.config.FiksApiProperties;
import no.ks.fiks.io.client.eksempel.config.FiksIOKlientProperties;
import no.ks.fiks.io.client.eksempel.config.MaskinportenProperties;
import no.ks.fiks.io.client.eksempel.utils.JavaClientUtils;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

import static no.ks.fiks.io.client.eksempel.AnsiColor.*;

public class EksempelApp {
    private static final Logger logger = LoggerFactory.getLogger(EksempelApp.class);
    private static final FiksIOKlientProperties klientProperties = FiksIOKlientProperties.loadProperties("config.properties");
    private static final MaskinportenProperties maskinportenProperties = MaskinportenProperties.loadMaskinportenProperties("maskinporten.properties");
    private static final FiksApiProperties fiksApiProperties = FiksApiProperties.loadProperties("fiks-api.properties");
    private static final AmqpProperties amqpProperties = AmqpProperties.loadProperties("amqp.properties");

    public static void main(String[] args) throws Exception {
        final var kontoId = klientProperties.kontoId();

        try (var javaKlient = JavaClientUtils.lagJavaKlient(klientProperties, maskinportenProperties, fiksApiProperties, amqpProperties)) {
            javaKlient.newSubscription(new MeldingHandler()::behandleMelding);

            runInteractiveConsole(javaKlient, kontoId);
        }
    }

    private static void runInteractiveConsole(FiksIOKlient javaKlient, KontoId mottakerKontoId) {
        final var avsenderKontoId = javaKlient.getKontoId();
        System.out.println("Starter interaktiv konsoll:");
        System.out.println("  P - Send PING melding");
        System.out.println("  G - Send PONG melding");
        System.out.println("  K - Hent konto informasjon og status");
        System.out.println("  Q - Avslutter applikasjonen");

        try (Scanner scanner = new Scanner(System.in)) {
            String input = "";

            while (scanner.hasNextLine() && !input.equalsIgnoreCase("Q")) {
                input = scanner.nextLine().trim().toUpperCase();
                switch (input) {
                    case "P":
                        logger.info(formatCommand("P trykket", "Sender PING melding fra konto: " + avsenderKontoId + " til konto: " + mottakerKontoId));
                        send(javaKlient, mottakerKontoId, "PING", "Dette er en PING melding");
                        break;
                    case "G":
                        logger.info(formatCommand("G trykket", "Sender PONG melding fra konto: " + avsenderKontoId + " til konto: " + mottakerKontoId));
                        send(javaKlient, mottakerKontoId, "PONG", "Dette er en PONG melding");
                        break;
                    case "K":
                        logger.info(formatCommand("K trykket", "Henter konto: " + mottakerKontoId));
                        final var konto = javaKlient.getKonto(mottakerKontoId);
                        konto.ifPresent(value -> logger.info(formatKonto(value)));
                        break;
                    case "Q":
                        logger.info(formatCommand("Q trykket", "Avslutter applikasjon"));
                        return;
                    default:
                        if (!input.isEmpty()) {
                            logger.info(formatCommand("Ukjent kommando", "Prøv P, G, K eller Q"));
                        }
                }
            }
        }
    }

    public static void send(FiksIOKlient klient, KontoId mottakerKontoId, String meldingType, String innhold) {
        final var avsenderKontoId = klient.getKontoId();
        logger.info("Sender melding - Avsender: {}, Mottaker: {}, Type: {}", avsenderKontoId, mottakerKontoId, meldingType);
        klient.send(MeldingRequest.builder().mottakerKontoId(mottakerKontoId).meldingType(meldingType).build(), innhold, "melding.txt");
    }

    private static String formatCommand(String command, String message) {
        return String.format("%s[%s]%s %s%s%s",
                BOLD + AnsiColor.MAGENTA, command, RESET,
                AnsiColor.WHITE, message, RESET
        );
    }

    private static String formatKonto(Konto konto) {
        return String.format(
            """

                %s════════════════════════════════════════%s
                %s  KONTO INFORMASJON%s
                %s════════════════════════════════════════%s
                %sKonto ID:%s            %s%s%s
                %sKonto Navn:%s          %s%s%s
                %sFiks Org Navn:%s       %s%s%s
                %sGyldig Avsender:%s     %s%s%s
                %sGyldig Mottaker:%s     %s%s%s
                %sUavhentede Meldinger:%s %s%d%s
                %s════════════════════════════════════════%s
                """,
            BOLD + BLUE, RESET,
            BOLD + GREEN, RESET,
            BOLD + BLUE, RESET,
            CYAN, RESET, YELLOW, konto.getKontoId(), RESET,
            CYAN, RESET, YELLOW, konto.getKontoNavn(), RESET,
            CYAN, RESET, YELLOW, konto.getFiksOrgNavn(), RESET,
            CYAN, RESET, YELLOW, konto.isGyldigAvsender(), RESET,
            CYAN, RESET, YELLOW, konto.isGyldigMottaker(), RESET,
            CYAN, RESET, YELLOW, konto.getAntallUavhentedeMeldinger(), RESET,
            BOLD + BLUE, RESET
        );
    }
}


