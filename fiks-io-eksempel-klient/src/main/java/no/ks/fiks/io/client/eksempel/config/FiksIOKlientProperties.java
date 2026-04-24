package no.ks.fiks.io.client.eksempel.config;

import com.google.common.io.Resources;
import no.ks.fiks.io.client.model.KontoId;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

public record FiksIOKlientProperties(
    String privatekeyFile,
    String offentligNokkel,
    KontoId kontoId,
    UUID systemId,
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
            String systemIdString = properties.getProperty("system.id", "").trim();
            UUID systemId = systemIdString.isEmpty() ?
                null :
                UUID.fromString(systemIdString);

            String publickeyFilename = properties.getProperty("publickey.file");
            String offentligNokkel;


            if(publickeyFilename == null || publickeyFilename.isEmpty()) {
                offentligNokkel = null;
            } else {
                offentligNokkel = loadPublicKeyContent(publickeyFilename);
            }



            return new FiksIOKlientProperties(
                properties.getProperty("privatekey.file"),
                offentligNokkel,
                new KontoId(UUID.fromString(properties.getProperty("konto.id"))),
                systemId,
                UUID.fromString(properties.getProperty("integrasjon.id")),
                properties.getProperty("integrasjon.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("Feil med properties i " + configFile, e);
        }
    }

    private static String loadPublicKeyContent(String filename) throws Exception {
        try (var fileInputStream = new FileInputStream(Resources.getResource(filename).getFile())) {
            return new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
