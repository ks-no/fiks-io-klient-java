package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;
import no.ks.fiks.io.client.Meta;

import java.util.Properties;

public record AmqpProperties(
    String host,
    Integer port,
    String applikasjonNavn,
    Integer mottakBufferStorrelse
) {
    public static AmqpProperties loadProperties(String configFile) {
        final var properties = new Properties();

        try (var inputStream = Resources.getResource(configFile).openStream()) {
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke hente properties fra " + configFile, e);
        }

        try {
            final String applikasjonNavnProp = properties.getProperty("amqp.applikasjonNavn");
            final String applikasjonNavn = applikasjonNavnProp != null && !applikasjonNavnProp.isEmpty()
                ? applikasjonNavnProp
                : String.format("Fiks IO klient (Java) %s", Meta.VERSJON);

            return new AmqpProperties(
                properties.getProperty("amqp.host"),
                Integer.parseInt(properties.getProperty("amqp.port")),
                applikasjonNavn,
                Integer.parseInt(properties.getProperty("amqp.mottakBufferStorrelse"))
            );
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }
}