package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.eksempel.utils.MeldingType;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static no.ks.fiks.io.client.eksempel.AnsiColor.*;
import static no.ks.fiks.io.client.eksempel.utils.MeldingType.PONG;

public class MeldingHandler {
    private final Logger logger = LoggerFactory.getLogger(MeldingHandler.class);


    public void behandleMelding(MottattMelding mottattMelding, SvarSender svarSender) {
        final var meldingId = mottattMelding.getMeldingId();
        final var meldingType = mottattMelding.getMeldingType();
        final var avsenderKontoId = mottattMelding.getAvsenderKontoId();
        final var mottakerKontoId = mottattMelding.getMottakerKontoId();
        final var headers = mottattMelding.getHeadere();

        logger.info(formatMeldingMottatt(meldingId, meldingType, avsenderKontoId, mottakerKontoId, headers));

        switch (MeldingType.from(meldingType)) {
            case PING:
                sendPong(meldingId, avsenderKontoId, mottakerKontoId, svarSender);
                break;
            case PONG:
                handlePong(meldingId, avsenderKontoId, mottakerKontoId, mottattMelding);
                break;
            default:
                logger.warn(formatUkjentMeldingType(meldingType));
        }

        svarSender.ack();
    }

    private static void lesOglogMeldingInnhold(MottattMelding mottattMelding) {
        try {
            mottattMelding.getDekryptertZipStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPong(MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId, SvarSender svarSender) {
        logger.info(formatSenderMelding(PONG, meldingId, avsenderKontoId, mottakerKontoId));
        try {
            svarSender.svar(PONG.toString(), "PONG", "pong.txt");
            logger.info(formatMeldingSendt(PONG, meldingId, avsenderKontoId, mottakerKontoId));
        } catch (Exception e) {
            logger.error(formatFeilSending(PONG, meldingId, avsenderKontoId, mottakerKontoId), e);
        }
    }

    private void handlePong(MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId, MottattMelding svarPaMelding) {
        if(svarPaMelding != null) {
            lesOglogMeldingInnhold(svarPaMelding);
            logger.info(formatMeldingSvarPa(PONG, meldingId, svarPaMelding.getMeldingId(), avsenderKontoId, mottakerKontoId));
        } else {
            logger.info(formatMeldingUtenSvar(PONG, meldingId, avsenderKontoId, mottakerKontoId));
        }
    }

    private static String formatMeldingMottatt(MeldingId meldingId, String meldingType, KontoId avsenderKontoId, KontoId mottakerKontoId, Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n%sMELDING MOTTATT%s | %sMeldingType:%s %s%s%s | %sMeldingId:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + MAGENTA, RESET,
                CYAN, RESET, YELLOW, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        ));

        if (headers != null && !headers.isEmpty()) {
            sb.append(String.format("\n%sHeaders:%s", CYAN, RESET));
            headers.forEach((key, value) ->
                sb.append(String.format("\n  %s%s:%s %s%s%s", CYAN, key, RESET, YELLOW, value, RESET))
            );
        }

        return sb.toString();
    }

    private static String formatSenderMelding(MeldingType meldingType, MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("\n%sSENDER %s%s | %sMeldingId:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + BLUE, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        );
    }

    private static String formatMeldingSendt(MeldingType meldingType, MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("\n%s%s SENDT VELLYKKET%s | %sMeldingId:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + GREEN, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        );
    }

    private static String formatFeilSending(MeldingType meldingType, MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("\n%sFEIL VED SENDING AV %s%s | %sMeldingId:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + RED, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        );
    }

    private static String formatMeldingSvarPa(MeldingType meldingType, MeldingId meldingId, MeldingId svarPaMelding, KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("\n%sMOTTATT %s SOM SVAR%s | %sMeldingId:%s %s%s%s | %sSvar på:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + MAGENTA, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, svarPaMelding, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        );
    }

    private static String formatMeldingUtenSvar(MeldingType meldingType, MeldingId meldingId, KontoId avsenderKontoId, KontoId mottakerKontoId) {
        return String.format("\n%sMOTTATT %s UTEN Å HA SENDT no.ks.fiks.arkiv.v1.ping%s | %sMeldingId:%s %s%s%s | %sAvsender:%s %s%s%s | %sMottaker:%s %s%s%s",
                BOLD + MAGENTA, meldingType, RESET,
                CYAN, RESET, YELLOW, meldingId, RESET,
                CYAN, RESET, YELLOW, avsenderKontoId, RESET,
                CYAN, RESET, YELLOW, mottakerKontoId, RESET
        );
    }

    private static String formatUkjentMeldingType(String meldingType) {
        return String.format("\n%s  UKJENT MELDINGSTYPE%s | %sMeldingType:%s %s%s%s",
                BOLD + RED, RESET,
                CYAN, RESET, YELLOW, meldingType, RESET
        );
    }
}