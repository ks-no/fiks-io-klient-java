package no.ks.fiks.io.client.eksempel;

import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeldingHandler {
    private final Logger logger = LoggerFactory.getLogger(MeldingHandler.class);

    public void behandleMelding(MottattMelding mottattMelding, SvarSender svarSender) {
        final var meldingId = mottattMelding.getMeldingId();
        final var meldingType = mottattMelding.getMeldingType();

        logger.info("Behandler melding. MeldingId: {}, MeldingType: {}", meldingId, meldingType);

        switch (meldingType) {
            case "PING":
                sendPong(meldingId, svarSender);
                break;
            case "PONG":
                handlePong(meldingId);
                break;
            default:
                logger.warn("Ukjent meldingstype: {}", meldingType);
        }

        svarSender.ack();
    }

    private void sendPong(MeldingId meldingId, SvarSender svarSender) {
        logger.info("Sender PONG. MeldingId: {}", meldingId);
        try {
            svarSender.svar("PONG", "PONG", "pong.txt");
            logger.info("PONG sendt successfully for MeldingId: {}", meldingId);
        } catch (Exception e) {
            logger.error("Feil ved sending av PONG for MeldingId: {}", meldingId, e);
        }
    }

    private void handlePong(MeldingId meldingId) {
        logger.info("Mottatt PONG som svar på PING. MeldingId: {}", meldingId);
    }
}
