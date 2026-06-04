package no.ks.fiks.io.client;

import no.ks.fiks.io.client.model.KontoId;

import java.time.Instant;

public interface PublicKeyUploadRateLimiter {
    boolean shouldUpload(KontoId kontoId);
    void recordUpload(KontoId kontoId);
    Instant nextUploadAllowedAfter(KontoId kontoId);
}
