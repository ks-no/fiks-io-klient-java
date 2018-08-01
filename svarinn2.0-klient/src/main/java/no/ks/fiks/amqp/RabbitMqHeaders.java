package no.ks.fiks.amqp;

public final class RabbitMqHeaders {

    private RabbitMqHeaders() { }

    public static final String AVSENDER_ID = "avsender-id";
    public static final String AVSENDER_NAVN = "avsender-navn";
    public static final String MELDING_TYPE = "type";
    public static final String SVAR_PA_MELDING_ID = "svar-til";

    public static final String DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String DEAD_LETTER_FIRST_DEATH_REASON = "x-first-death-reason";
    public static final String DEAD_LETTER_FIRST_DEATH_QUEUE = "x-first-death-queue";
}
