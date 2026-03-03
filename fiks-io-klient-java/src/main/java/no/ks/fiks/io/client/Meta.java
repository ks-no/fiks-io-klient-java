package no.ks.fiks.io.client;

import java.io.IOException;
import java.util.Properties;

public class Meta {
    /**
     * Inneholder gjeldende versjonsnummer
     */
    public static String VERSJON;
    private static final String VERSJON_PROP_FIL = "fiks-io-version.properties";

    private static final String PROP_VERSJON = "versjon";

    static {
        Properties properties = new Properties();
        try {
            properties.load(Meta.class.getClassLoader().getResourceAsStream(VERSJON_PROP_FIL));
            VERSJON = properties.getProperty(PROP_VERSJON);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Kan ikke laste versjonsfil %s", VERSJON_PROP_FIL), e);
        }
    }
}
