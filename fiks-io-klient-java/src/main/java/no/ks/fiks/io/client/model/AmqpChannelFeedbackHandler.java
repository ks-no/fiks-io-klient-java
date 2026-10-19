package no.ks.fiks.io.client.model;

import lombok.Builder;

@Builder
public class AmqpChannelFeedbackHandler {

    private final Runnable handleAck;

    private final Runnable handleNack;

    @Deprecated(since = "3.9.0")
    private final Runnable handNackWithRequeue;

    private final Runnable handleRejectWithRequeue;

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

    public Runnable getHandleRejectWithRequeue() {
        if (handleRejectWithRequeue == null) {
            throw new UnsupportedOperationException("\"rejectWithRequeue\" has not been implemented");
        }
        return handleRejectWithRequeue;
    }

    /**
     *
     * <p> Use {@link #getHandleRejectWithRequeue()} instead. </p>
     */
    @Deprecated(since = "3.9.0")
    public Runnable getHandleNackWithRequeue() {
        if (handNackWithRequeue == null) {
            throw new UnsupportedOperationException("\"nackWithRequeue\" has not been implemented");
        }
        return handNackWithRequeue;
    }
}
