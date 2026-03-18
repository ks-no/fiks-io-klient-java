package no.ks.fiks.io.client.eksempel.utils;

public enum MeldingType {
    PING("no.ks.fiks.arkiv.v1.ping"),
    PONG("no.ks.fiks.arkiv.v1.pong"),
    UKJENT("");

    public final String part;

    MeldingType(String part) {
        this.part = part;
    }

    @Override
    public String toString() {
        return part;
    }

    public static MeldingType from(String meldingType) {
        for (MeldingType type : MeldingType.values()) {
            if (type.part.equals(meldingType)) {
                return type;
            }
        }
        return UKJENT;
    }
}