package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;
import no.ks.fiks.io.client.model.KontoId;

import java.util.Properties;
import java.util.UUID;

public record FiksIOKlientProperties(
    String privatekeyFile,
    KontoId kontoId,
    UUID integrasjonId,
    String integrasjonPassword
) {
    public static FiksIOKlientProperties loadProperties(String configFile) {
        final var properties = new Properties();

        try (var inputStream = Resources.getResource(configFile).openStream()) {
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke hente properties fra " + configFile, e);
        }

        try {
            return new FiksIOKlientProperties(
                properties.getProperty("privatekey.file"),
                new KontoId(UUID.fromString(properties.getProperty("konto.id"))),
                UUID.fromString(properties.getProperty("integrasjon.id")),
                properties.getProperty("integrasjon.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }
}
