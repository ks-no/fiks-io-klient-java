package no.ks.fiks.io.client.model;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class MeldingId {
    @NonNull UUID uuid;

    @Override
    public String toString(){
        return uuid.toString();
    }
}
