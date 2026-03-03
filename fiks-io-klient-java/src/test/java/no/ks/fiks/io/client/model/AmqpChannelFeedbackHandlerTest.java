package no.ks.fiks.io.client.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmqpChannelFeedbackHandlerTest {

    @DisplayName("Missing handler")
    @Nested
    class NotImplemented {
        @DisplayName("for ACK")
        @Test
        void getHandleAck() {
            assertThrows(UnsupportedOperationException.class, () -> AmqpChannelFeedbackHandler.builder().build().getHandleAck());
        }

        @DisplayName("for NACK")
        @Test
        void getHandleNack() {
            assertThrows(UnsupportedOperationException.class, () -> AmqpChannelFeedbackHandler.builder().build().getHandleNack());
        }

        @DisplayName("for NACK W/REQUEUE ")
        @Test
        void getHandNackWithRequeue() {
            assertThrows(UnsupportedOperationException.class, () -> AmqpChannelFeedbackHandler.builder().build().getHandleNackWithRequeue());
        }
    }

    @DisplayName("Handler defined")
    @Nested
    class HasHandler {

        @DisplayName("for ACK")
        @Test
        public void ack() {
            assertNotNull(AmqpChannelFeedbackHandler.builder().handleAck(() -> {
            }).build().getHandleAck());
        }

        @DisplayName("for NACK")
        @Test
        public void nack() {
            assertNotNull(AmqpChannelFeedbackHandler.builder().handleNack(() -> {
            }).build().getHandleNack());
        }


        @DisplayName("for ACK")
        @Test
        public void nackWithRequeue() {
            assertNotNull(AmqpChannelFeedbackHandler.builder().handNackWithRequeue(() -> {
            }).build().getHandleNackWithRequeue());
        }
    }


}