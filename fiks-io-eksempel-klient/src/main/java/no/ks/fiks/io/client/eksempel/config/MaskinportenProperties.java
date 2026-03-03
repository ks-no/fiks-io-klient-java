package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;

import java.util.Properties;
import java.util.UUID;

public record MaskinportenProperties(
    UUID klientId
) {
    public static MaskinportenProperties loadMaskinportenProperties(String configFile) {
        try {
            try (var inputStream = Resources.getResource(configFile).openStream()) {
                final var properties = new Properties();
                properties.load(inputStream);

                return new MaskinportenProperties(
                    UUID.fromString(properties.getProperty("klient.id"))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }
}
