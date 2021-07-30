package no.ks.fiks.io.client.model.feilmelding;

public abstract class Feilmelding {

    public String errorId;
    public String feilmelding;
    public String correlationId;

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
