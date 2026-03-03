package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;

import java.util.Properties;
import java.util.UUID;

public record MaskinportenProperties(
    String audience,
    String tokenEndpoint,
    UUID klientId
) {
    public static MaskinportenProperties loadMaskinportenProperties(String configFile) {
        try {
            try (var inputStream = Resources.getResource(configFile).openStream()) {
                final var properties = new Properties();
                properties.load(inputStream);

                return new MaskinportenProperties(
                    properties.getProperty("maskinporten.audience"),
                    properties.getProperty("maskinporten.token-endpoint"),
                    UUID.fromString(properties.getProperty("klient.id"))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }
}
