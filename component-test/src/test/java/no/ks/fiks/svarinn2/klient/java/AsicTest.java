/*
package no.ks.fiks.svarinn2.klient.java;

import no.ks.fiks.svarinn.client.AsicHandler;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsicTest {

    @Test
    void name() throws Exception{
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/" + "kommune1.p12"), "123456".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("kommune1keyalias");

        AsicHandler asicHandler = new AsicHandler((PrivateKey) keyStore.getKey("kommune1keyalias", "123456".toCharArray()));

        String input = "Hei!";
        File encrypt = asicHandler.encrypt(cert, new ByteArrayInputStream(input.getBytes()));
        String output = new String(asicHandler.decrypt(FileUtils.readFileToByteArray(encrypt)));
        assertEquals(input, output);
    }
}
*/
