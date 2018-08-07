package no.ks.fiks.amqp;

public final class MeldingsType {

    public static final String KVITTERING_BAD_REQUEST = "kvittering.badrequest";

    private MeldingsType() { }

    public static final String KVITTERING_MOTTATT = "kvittering.mottatt";
    public static final String KVITTERING_FEILET = "kvittering.feilet";

    public static final String KVITTERING_TIMEOUT = "kvittering.timeout";
}