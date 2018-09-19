package no.ks.fiks.svarinn.client.model;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class FiksOrgId {
    @NonNull UUID fiksOrgId;
}
