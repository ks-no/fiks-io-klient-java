package no.ks.fiks.svarinn2.klient.java;

import org.apache.commons.lang3.RandomStringUtils;

public class TestUtil {

    public static String randomFnr() {
        return RandomStringUtils.random(11, false, true);
    }
}
