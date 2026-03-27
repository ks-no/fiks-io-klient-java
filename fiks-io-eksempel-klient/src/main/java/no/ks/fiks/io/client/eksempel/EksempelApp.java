package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.eksempel.api.FiksArkivApiHandler;
import no.ks.fiks.io.client.eksempel.api.ProtokollKonfigurasjonKlient;
import no.ks.fiks.io.client.eksempel.config.AmqpProperties;
import no.ks.fiks.io.client.eksempel.config.FiksApiProperties;
import no.ks.fiks.io.client.eksempel.config.FiksIOKlientProperties;
import no.ks.fiks.io.client.eksempel.config.MaskinportenProperties;
import no.ks.fiks.io.client.eksempel.utils.JavaClientUtils;
import no.ks.fiks.io.client.eksempel.utils.MeldingType;
import no.ks.fiks.io.client.eksempel.utils.TokenProvider;
import no.ks.fiks.io.client.model.KlientKorrelasjonId;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingRequest;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.ProtokollKontoResponse;
import no.ks.fiks.protokoll.konfigurasjon.v1.model.ProtokollSystemSummaryWithOrgNameResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static no.ks.fiks.io.client.eksempel.AnsiColor.*;
import static no.ks.fiks.io.client.eksempel.utils.MeldingType.PING;
import static no.ks.fiks.io.client.eksempel.utils.MeldingType.PONG;

public class EksempelApp {
    private static final Logger logger = LoggerFactory.getLogger(EksempelApp.class);
    private static final FiksIOKlientProperties klientProperties = FiksIOKlientProperties.loadProperties("config.properties");
    private static final MaskinportenProperties maskinportenProperties = MaskinportenProperties.loadMaskinportenProperties("maskinporten.properties");
    private static final FiksApiProperties fiksApiProperties = FiksApiProperties.loadProperties("fiks-api.properties");
    private static final AmqpProperties amqpProperties = AmqpProperties.loadProperties("amqp.properties");
    private static FiksArkivApiHandler apiHandler;
    private static ProtokollKontoResponse opprettetFiksArkivKonto = null;


    public static void main(String[] args) throws Exception {
        final var mottakerKontoId = klientProperties.kontoId();

        try (var javaKlient = JavaClientUtils.lagJavaKlient(klientProperties, maskinportenProperties, fiksApiProperties, amqpProperties)) {
            final var tokenProvider = JavaClientUtils.lagTokenProvider(maskinportenProperties);

            final var protokollKonfigurasjonKlient = new ProtokollKonfigurasjonKlient(tokenProvider, klientProperties.integrasjonId(), klientProperties.integrasjonPassword(), fiksApiProperties);

            apiHandler = new FiksArkivApiHandler(protokollKonfigurasjonKlient);

            javaKlient.newSubscription(new MeldingHandler()::behandleMelding);

            runInteractiveConsole(klientProperties.systemId(), javaKlient, mottakerKontoId, tokenProvider);
            System.exit(0);
        }
    }

    private static void runInteractiveConsole(UUID protokollSystemId, FiksIOKlient javaKlient, KontoId mottakerKontoId, TokenProvider tokenProvider) {
        final var avsenderKontoId = javaKlient.getKontoId();

        try (Scanner scanner = new Scanner(System.in)) {
            String input = "";
            printKommandoMeny();
            while (scanner.hasNextLine() && !input.equalsIgnoreCase("Q")) {
                input = scanner.nextLine().trim().toUpperCase();
                switch (input) {
                    case "P":
                        logger.info(formatCommand("P trykket", "Sender PING melding fra konto: " + avsenderKontoId + " til konto: " + mottakerKontoId));
                        send(javaKlient, mottakerKontoId, PING, "Dette er en PING melding");
                        break;
                    case "G":
                        logger.info(formatCommand("G trykket", "Sender PONG melding fra konto: " + avsenderKontoId + " til konto: " + mottakerKontoId));
                        send(javaKlient, mottakerKontoId, PONG, "Dette er en PONG melding");
                        break;
                    case "K": {
                        logger.info(formatCommand("K trykket", "Henter konto: " + avsenderKontoId));
                        final var konto = javaKlient.getKonto(avsenderKontoId);
                        konto.ifPresent(value -> logger.info(formatKonto(value)));
                        break;
                    }
                    case "M":
                        logger.info(formatCommand("M trykket", "Henter maskinporten token"));
                        hentOgVisMaskinportenToken(tokenProvider);
                        break;
                    case "N":
                        if(opprettetFiksArkivKonto != null) {
                            logger.info(formatCommand("N trykket", "Fiks Arkiv Konto er allerede opprettet"));
                            break;
                        }

                        if(protokollSystemId == null) {
                            logger.info("Kan ikke opprette Fiks Arkiv Konto da SystemId ikke er satt");
                            return;
                        }

                        logger.info(formatCommand("N trykket", "Opprettelse av Fiks Arkiv konto"));
                        final var konto = javaKlient.getKonto(avsenderKontoId);
                        final var fiksOrgId = konto.get().getFiksOrgId();

                        opprettetFiksArkivKonto = opprettFiksArkivKonto(fiksOrgId.getFiksOrgId(), protokollSystemId, klientProperties.offentligNokkel());
                        break;
                    case "T":
                        if (opprettetFiksArkivKonto == null) {
                            logger.info(formatCommand("T trykket", "Fiks Arkiv Konto er ikke opprettet ennå. Opprett først med kommando N"));
                            break;
                        }
                        logger.info(formatCommand("T trykket", "Sender tilgangsforespørsel til opprettet Fiks Arkiv konto"));

                        final var kontoForTilgang = javaKlient.getKonto(avsenderKontoId);
                        final var fiksOrgIdForTilgang = kontoForTilgang.get().getFiksOrgId();
                        final var nyKontoId = new KontoId(opprettetFiksArkivKonto.getId());
                        beOmTilgangTilKonto(fiksOrgIdForTilgang.getFiksOrgId(), protokollSystemId, nyKontoId, avsenderKontoId);
                        break;
                    case "S":
                        if (opprettetFiksArkivKonto == null) {
                            logger.info(formatCommand("S trykket", "Fiks Arkiv Konto er ikke opprettet ennå. Opprett først med kommando N"));
                            break;
                        }
                        logger.info(formatCommand("S trykket", "Henter alle tilgangforspørslene"));

                        final var kontoForTilgangForesporsler = javaKlient.getKonto(avsenderKontoId);
                        final var fiksOrgIdForForesporsler = kontoForTilgangForesporsler.get().getFiksOrgId();
                        final var kontoIdForForesporsler = new KontoId(opprettetFiksArkivKonto.getId());
                        seTilgangForesporsler(fiksOrgIdForForesporsler.getFiksOrgId(), protokollSystemId, kontoIdForForesporsler);
                        break;
                    case "A":
                        if (opprettetFiksArkivKonto == null) {
                            logger.info(formatCommand("A trykket", "Fiks Arkiv Konto er ikke opprettet ennå. Opprett først med kommando N"));
                            break;
                        }
                        logger.info(formatCommand("A trykket", "Godkjenner alle tilgangforspørslene"));

                        final var kontoForGodkjenning = javaKlient.getKonto(avsenderKontoId);
                        final var fiksOrgIdForGodkjenning = kontoForGodkjenning.get().getFiksOrgId();
                        final var kontoIdForGodkjenning = new KontoId(opprettetFiksArkivKonto.getId());
                        godkjennAlleTilgangForesporsler(fiksOrgIdForGodkjenning.getFiksOrgId(), protokollSystemId, kontoIdForGodkjenning);
                        break;
                    case "Q":
                        logger.info(formatCommand("Q trykket", "Avslutter applikasjon"));
                        return;
                    default:
                        if (!input.isEmpty()) {
                            System.out.printf("\nUkjent kommando: %s\n%n", input);
                        }
                }
                try {
                    Thread.sleep(100);
                    printKommandoMeny();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void send(FiksIOKlient klient, KontoId mottakerKontoId, MeldingType meldingType, String innhold) {
        send(klient, mottakerKontoId, meldingType, innhold, null);
    }

    public static void send(FiksIOKlient klient, KontoId mottakerKontoId, MeldingType meldingType, String innhold, String korrelasjonId) {
        final var avsenderKontoId = klient.getKontoId();
        logger.info("Sender melding - Avsender: {}, Mottaker: {}, Type: {}", avsenderKontoId, mottakerKontoId, meldingType);
        final var meldingRequest = MeldingRequest.builder()
            .mottakerKontoId(mottakerKontoId)
            .meldingType(meldingType.toString())
            .korrelasjonsId(new KlientKorrelasjonId(korrelasjonId))
            .build();

        klient.send(meldingRequest, innhold, "melding.txt");
    }

    private static ProtokollKontoResponse opprettFiksArkivKonto(UUID fiksOrgId, UUID systemId, String offentligNokkel) {
        final var nyKonto = apiHandler.opprettFiksArkivKonto(fiksOrgId, systemId, offentligNokkel);
        final var nyKontoId = new KontoId(nyKonto.getId());

        if (nyKonto != null) {
            logger.info(formatCommand("Fiks Arkiv Konto Opprettet", "Ny KontoId: " + nyKontoId));
            return nyKonto;
        } else {
            logger.error(formatCommand("Feil", "Kunne ikke opprette Fiks Arkiv konto"));
            return null;
        }
    }

    private static void seTilgangForesporsler(UUID fiksOrgId, UUID systemId, KontoId kontoId) {
        final var tilgangerForespurt = apiHandler.seTilgangForesporsler(fiksOrgId, systemId, kontoId.getUuid());
        logger.info(formatTilgangForesporsler(tilgangerForespurt));
    }

    private static void godkjennAlleTilgangForesporsler(UUID fiksOrgId, UUID systemId, KontoId kontoId) {
        apiHandler.godkjennTilgangsforesporsel(fiksOrgId, systemId, kontoId);
        logger.info(formatCommand("Tilgangsforespørsel Godkjent", "Godkjent på vegne av konto: " + kontoId));
    }

    private static void beOmTilgangTilKonto(UUID fiksOrgId, UUID systemId, KontoId nyKontoId, KontoId avsenderKontoId) {
        apiHandler.beOmTilgang(fiksOrgId, systemId, nyKontoId);
        logger.info(formatTilgangsforespoersel(avsenderKontoId, nyKontoId));
    }

    private static void hentOgVisMaskinportenToken(TokenProvider tokenProvider) {
        final var token = tokenProvider.getMaskinportenToken();
        logger.info(formatMaskinportenToken(token));
    }

    private static String formatCommand(String command, String message) {
        return String.format("%s[%s]%s %s%s%s",
                BOLD + AnsiColor.MAGENTA, command, RESET,
                AnsiColor.WHITE, message, RESET
        );
    }

    private static String formatMaskinportenToken(String token) {
        return String.format(
            """

                %s════════════════════════════════════════%s
                %s  MASKINPORTEN TOKEN%s
                %s════════════════════════════════════════%s
                %sToken:%s
                %s%s%s
                %s════════════════════════════════════════%s
                """,
            BOLD + BLUE, RESET,
            BOLD + GREEN, RESET,
            BOLD + BLUE, RESET,
            CYAN, RESET,
            YELLOW, token, RESET,
            BOLD + BLUE, RESET
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

    private static String formatTilgangForesporsler(List<ProtokollSystemSummaryWithOrgNameResponse> tilganger) {
        var sb = new StringBuilder();
        sb.append(String.format("""

            %s════════════════════════════════════════%s
            %s  FORESPURTE TILGANGER (%d)%s
            %s════════════════════════════════════════%s
            """,
            BOLD + BLUE, RESET,
            BOLD + GREEN, tilganger.size(), RESET,
            BOLD + BLUE, RESET
        ));

        for (int i = 0; i < tilganger.size(); i++) {
            sb.append(formatTilgangItem(tilganger.get(i), i + 1));
            if (i < tilganger.size() - 1) {
                sb.append(String.format("%s─────────────────────────────────────%s\n", BLUE, RESET));
            }
        }

        sb.append(String.format("%s════════════════════════════════════════%s\n", BOLD + BLUE, RESET));
        return sb.toString();
    }

    private static String formatTilgangItem(ProtokollSystemSummaryWithOrgNameResponse tilgang, int index) {
        return String.format("""
            %s[%d]%s
            %sID:%s              %s%s%s
            %sNavn:%s            %s%s%s
            %sBeskrivelse:%s     %s%s%s
            %sFiks Org Navn:%s   %s%s%s
            """,
            BOLD + CYAN, index, RESET,
            CYAN, RESET, YELLOW, tilgang.getId(), RESET,
            CYAN, RESET, YELLOW, tilgang.getNavn(), RESET,
            CYAN, RESET, YELLOW, tilgang.getBeskrivelse(), RESET,
            CYAN, RESET, YELLOW, tilgang.getFiksOrgNavn(), RESET
        );
    }

    private static String formatTilgangsforespoersel(KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("%s[Tilgangsforespørsel]%s Konto %s%s%s ber om tilgang til å snakke med konto: %s%s%s",
                BOLD + GREEN, RESET,
                YELLOW, avsenderKontoId, RESET,
                YELLOW, mottakerKontoId, RESET
        );
    }

    private static void printKommandoMeny() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║             TILGJENGELIGE KOMMANDOER                           ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║  MELDINGER:                                                    ║");
        System.out.println("║    P - Send PING melding                                       ║");
        System.out.println("║    G - Send PONG melding                                       ║");
        System.out.println("║                                                                ║");
        System.out.println("║  KONTO OG TILGANG:                                             ║");
        System.out.println("║    K - Hent konto informasjon og status                        ║");
        System.out.println("║    M - Hent maskinporten token                                 ║");
        System.out.println("║                                                                ║");
        System.out.println("║  FIKS ARKIV KONTO:                                             ║");
        System.out.println("║    N - Opprett Fiks Arkiv konto som arkiv                      ║");
        System.out.println("║    T - Send tilgangsforespørsel til opprettet konto            ║");
        System.out.println("║    S - Se alle tilgangforspørslene                             ║");
        System.out.println("║    A - Godkjenn alle tilgangforspørslene                       ║");
        System.out.println("║                                                                ║");
        System.out.println("║  SYSTEM:                                                       ║");
        System.out.println("║    Q - Avslutter applikasjonen                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
    }
}


