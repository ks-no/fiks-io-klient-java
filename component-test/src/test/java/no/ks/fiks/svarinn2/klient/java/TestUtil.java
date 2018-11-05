package no.ks.fiks.svarinn2.klient.java;

import no.ks.fiks.componenttest.support.feign.Person;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtil {

    public static String randomFnr() {
        return RandomStringUtils.random(11, false, true);
    }

    public static KeyStore readP12(InputStream p12inputStream, String keystorePassword){
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(p12inputStream, keystorePassword.toCharArray());
            return keyStore;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static Identifikator randomOrgNoIdentifikator() {
        return new Identifikator()
                .identifikatorType(Identifikator.IdentifikatorTypeEnum.ORG_NO)
                .identifikator(randomOrgNo());
    }

    public static String randomOrgNo() {
        return RandomStringUtils.random(9, false, true);
    }

    public static byte[] randomBytes() {
        return randomBytes(0);
    }

    public static byte[] randomBytes(int length) {
        final byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public static Person randomPerson() {
        return Person.builder()
                .fnr(randomFnr())
                .orgno(randomOrgNo())
                .build();
    }
}
