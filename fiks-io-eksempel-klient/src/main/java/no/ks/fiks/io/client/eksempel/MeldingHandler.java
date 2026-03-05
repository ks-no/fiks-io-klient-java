package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.ks.fiks.io.client.eksempel.AnsiColor.*;

public class MeldingHandler {
    private final Logger logger = LoggerFactory.getLogger(MeldingHandler.class);

    public void behandleMelding(MottattMelding mottattMelding, SvarSender svarSender) {
        final var meldingId = mottattMelding.getMeldingId();
        final var meldingType = mottattMelding.getMeldingType();

        logger.info(formatMeldingMottatt(meldingId, meldingType));

        switch (meldingType) {
            case "PING":
                sendPong(meldingId, svarSender);
                break;
            case "PONG":
                handlePong(meldingId, mottattMelding.getSvarPaMelding());
                break;
            default:
                logger.warn(formatUkjentMeldingType(meldingType));
        }

        svarSender.ack();
    }

    private void sendPong(MeldingId meldingId, SvarSender svarSender) {
        logger.info(formatSenderMelding("PONG", meldingId));
        try {
            svarSender.svar("PONG", "PONG", "pong.txt");
            logger.info(formatMeldingSendt("PONG", meldingId));
        } catch (Exception e) {
            logger.error(formatFeilSending("PONG", meldingId), e);
        }
    }

    private void handlePong(MeldingId meldingId, MeldingId svarPaMelding) {
        if(svarPaMelding != null) {
            logger.info(formatMeldingSvarPa("PONG", meldingId, svarPaMelding));
        } else {
            logger.info(formatMeldingUtenSvar("PONG", meldingId));
        }
    }

    private static String formatMeldingMottatt(MeldingId meldingId, String meldingType) {
        return String.format("\n%sMELDING MOTTATT%s | %sMeldingType:%s %s%s%s | %sMeldingId:%s %s%s%s",
                BOLD + MAGENTA, RESET,
                CYAN, RESET, YELLOW, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET
        );
    }

    private static String formatSenderMelding(String meldingType, MeldingId meldingId) {
        return String.format("\n%sSENDER %s%s | %sMeldingId:%s %s%s%s",
                BOLD + BLUE, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET
        );
    }

    private static String formatMeldingSendt(String meldingType, MeldingId meldingId) {
        return String.format("\n%s%s SENDT VELLYKKET%s | %sMeldingId:%s %s%s%s",
                BOLD + GREEN, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET
        );
    }

    private static String formatFeilSending(String meldingType, MeldingId meldingId) {
        return String.format("\n%sFEIL VED SENDING AV %s%s | %sMeldingId:%s %s%s%s",
                BOLD + RED, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET
        );
    }

    private static String formatMeldingSvarPa(String meldingType, MeldingId meldingId, MeldingId svarPaMelding) {
        return String.format("\n%sMOTTATT %s SOM SVAR%s | %sMeldingId:%s %s%s%s | %sSvar på:%s %s%s%s",
                BOLD + MAGENTA, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, svarPaMelding, RESET
        );
    }

    private static String formatMeldingUtenSvar(String meldingType, MeldingId meldingId) {
        return String.format("\n%sMOTTATT %s UTEN PING%s | %sMeldingId:%s %s%s%s",
                BOLD + MAGENTA, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET
        );
    }

    private static String formatUkjentMeldingType(String meldingType) {
        return String.format("\n%s  UKJENT MELDINGSTYPE%s | %sMeldingType:%s %s%s%s",
                BOLD + RED, RESET,
                CYAN, RESET, YELLOW, meldingType, RESET
        );
    }
}