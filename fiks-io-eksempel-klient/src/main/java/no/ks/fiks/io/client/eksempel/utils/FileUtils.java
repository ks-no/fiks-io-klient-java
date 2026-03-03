package no.ks.fiks.io.client.eksempel.utils;

import com.google.common.io.Resources;

import java.io.File;
import java.security.KeyStore;

public class FileUtils {
    public static File fileFromResource(final String resourceName) {
        return org.apache.commons.io.FileUtils.toFile(Resources.getResource(resourceName));
    }

    public static KeyStore getKeyStore(String name, char[] password) {
        if (name == null) return null;

        try {
            try (var inputStream = Resources.getResource(name).openStream()) {
                KeyStore jks = KeyStore.getInstance("pkcs12");
                jks.load(inputStream, password);
                return jks;
            }
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke laste p12 " + name, e);
        }
    }
}
