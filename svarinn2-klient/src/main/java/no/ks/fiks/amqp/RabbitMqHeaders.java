package no.ks.fiks.amqp;

public final class RabbitMqHeaders {

    private RabbitMqHeaders() { }

    public static final String KVITTERING_FOR_MELDING = "kvittering-for-melding";
    public static final String AVSENDER_ID = "avsender-id";
    public static final String MELDING_ID = "melding-id";
    public static final String AVSENDER_NAVN = "avsender-navn";
    public static final String MELDING_TYPE = "type";
    public static final String SVAR_PA_MELDING_ID = "svar-til";

}
