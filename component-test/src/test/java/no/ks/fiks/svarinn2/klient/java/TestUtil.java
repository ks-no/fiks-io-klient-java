package no.ks.fiks.svarinn2.klient.java;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Identifikator;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtil {

    public static String randomFnr() {
        return RandomStringUtils.random(11, false, true);
    }

    public static Tuple2<X509Certificate, PrivateKey> readP12(InputStream p12inputStream, String password, String alias){
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(p12inputStream, password.toCharArray());
            return Tuple.of((X509Certificate) keyStore.getCertificate(alias), (PrivateKey) keyStore.getKey(alias, password.toCharArray()));
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
}
