package no.ks.fiks.io.client.model;

import jakarta.annotation.Nullable;
import lombok.Value;

@Value
public class KlientKorrelasjonId {
    @Nullable
    String klientKorrelasjonId;

    @Override
    public String toString() {
        return klientKorrelasjonId;
    }
}
