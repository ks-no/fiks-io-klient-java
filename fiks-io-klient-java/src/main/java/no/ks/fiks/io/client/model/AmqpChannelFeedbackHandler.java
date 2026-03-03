package no.ks.fiks.io.client.model;

import lombok.Builder;

@Builder
public class AmqpChannelFeedbackHandler {

    private final Runnable handleAck;

    private final Runnable handleNack;

    private final Runnable handNackWithRequeue;

    public Runnable getHandleAck() {
        if (handleAck == null) {
            throw new UnsupportedOperationException("\"ack\" has not been implemented");
        }
        return handleAck;
    }

    public Runnable getHandleNack() {
        if (handleNack == null) {
            throw new UnsupportedOperationException("\"nack\" has not been implemented");
        }
        return handleNack;
    }

    public Runnable getHandleNackWithRequeue() {
        if (handNackWithRequeue == null) {
            throw new UnsupportedOperationException("\"nackWithRequeue\" has not been implemented");
        }
        return handNackWithRequeue;
    }
}
