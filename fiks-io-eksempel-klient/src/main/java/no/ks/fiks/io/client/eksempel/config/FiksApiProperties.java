package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;

import java.util.Properties;

public record FiksApiProperties(
    String host,
    Integer port,
    String scheme
) {
    public static FiksApiProperties loadProperties(String configFile) {
        final var properties = new Properties();

        try (var inputStream = Resources.getResource(configFile).openStream()) {
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke hente properties fra " + configFile, e);
        }

        try {
            return new FiksApiProperties(
                properties.getProperty("fiks-api.host"),
                Integer.parseInt(properties.getProperty("fiks-api.port")),
                properties.getProperty("fiks-api.scheme")
            );
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }
}