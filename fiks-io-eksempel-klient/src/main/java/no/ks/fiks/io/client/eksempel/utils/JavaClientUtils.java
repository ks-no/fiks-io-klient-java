package no.ks.fiks.io.client.eksempel.utils;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.FiksIOKlientFactory;
import no.ks.fiks.io.client.eksempel.config.FiksIOKlientProperties;
import no.ks.fiks.io.client.eksempel.config.MaskinportenProperties;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static no.ks.fiks.io.client.eksempel.utils.FileUtils.fileFromResource;
import static no.ks.fiks.io.client.eksempel.utils.FileUtils.getKeyStore;

public class JavaClientUtils {

    public static FiksIOKlient lagJavaKlient(FiksIOKlientProperties klientProperties, MaskinportenProperties maskinportenProperties) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        final var virksomhetssertifikatKonfigurasjon = settOppVirksomhetssertifikatKonfigurasjon(klientProperties);
        final var privateKey = setOppPrivateKey(klientProperties);
        final var kontoKonfigurasjon = settOppKontoKonfigurasjon(klientProperties, privateKey);
        final var konfigurasjon = settOppFiksIOKonfigurasjon(klientProperties, maskinportenProperties, kontoKonfigurasjon, virksomhetssertifikatKonfigurasjon);

        return lagJavaKlient(konfigurasjon);
    }

    public static FiksIOKlient lagJavaKlient(FiksIOKonfigurasjon konfigurasjon) {
        return new FiksIOKlientFactory(konfigurasjon).build();
    }

    private static KontoKonfigurasjon settOppKontoKonfigurasjon(FiksIOKlientProperties klientProperties, PrivateKey privateKey) {
        return KontoKonfigurasjon.builder().kontoId(klientProperties.kontoId()).privatNokkel(privateKey).build();
    }

    private static FiksIOKonfigurasjon settOppFiksIOKonfigurasjon(FiksIOKlientProperties klientProperties, MaskinportenProperties maskinportenProperties, KontoKonfigurasjon kontoKonfigurasjon, VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {
        return FiksIOKonfigurasjon.defaultTestConfiguration(maskinportenProperties.klientId().toString(), klientProperties.integasjonId(), klientProperties.integasjonPassword(), kontoKonfigurasjon, virksomhetssertifikatKonfigurasjon);
    }

    private static PrivateKey setOppPrivateKey(FiksIOKlientProperties klientProperties) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(((PrivateKeyInfo) new PEMParser(new FileReader(fileFromResource(klientProperties.privatekeyFile()))).readObject()).getEncoded()));
    }

    private static VirksomhetssertifikatKonfigurasjon settOppVirksomhetssertifikatKonfigurasjon(FiksIOKlientProperties klientProperties) {
        return VirksomhetssertifikatKonfigurasjon.builder()
            .keyStore(getKeyStore(klientProperties.keystoreFile(), klientProperties.keystorePassword().toCharArray()))
            .keyAlias(klientProperties.keystorePrivatekeyAlias())
            .keyStorePassword(klientProperties.keystorePassword())
            .keyPassword(klientProperties.keystorePrivatekeyPassword())
            .build();
    }
}
