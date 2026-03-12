package no.ks.fiks.io.client.eksempel.utils;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.FiksIOKlientFactory;
import no.ks.fiks.io.client.eksempel.config.AmqpProperties;
import no.ks.fiks.io.client.eksempel.config.FiksApiProperties;
import no.ks.fiks.io.client.eksempel.config.FiksIOKlientProperties;
import no.ks.fiks.io.client.eksempel.config.MaskinportenProperties;
import no.ks.fiks.io.client.konfigurasjon.AmqpKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksApiKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.IdPortenKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static no.ks.fiks.io.client.eksempel.utils.FileUtils.fileFromResource;
import static no.ks.fiks.io.client.eksempel.utils.FileUtils.getKeyStore;

public class JavaClientUtils {

    public static FiksIOKlient lagJavaKlient(FiksIOKlientProperties klientProperties, MaskinportenProperties maskinportenProperties, FiksApiProperties fiksApiProperties, AmqpProperties amqpProperties) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        final var virksomhetssertifikatKonfigurasjon = settOppVirksomhetssertifikatKonfigurasjon(klientProperties);
        final var privateKey = setOppPrivateKey(klientProperties);
        final var kontoKonfigurasjon = settOppKontoKonfigurasjon(klientProperties, privateKey);
        final var konfigurasjon = settOppFiksIOKonfigurasjon(klientProperties, maskinportenProperties, fiksApiProperties, amqpProperties, kontoKonfigurasjon, virksomhetssertifikatKonfigurasjon);

        return lagJavaKlient(konfigurasjon);
    }

    public static FiksIOKlient lagJavaKlient(FiksIOKonfigurasjon konfigurasjon) {
        return new FiksIOKlientFactory(konfigurasjon).build();
    }

    public static TokenProvider lagTokenProvider(MaskinportenProperties maskinportenProperties, FiksIOKlientProperties fiksIOKlientProperties) {
        final Maskinportenklient maskinportenKlient = getMaskinportenKlient(maskinportenProperties, fiksIOKlientProperties);
        return new TokenProvider(maskinportenKlient);
    }

    public static KontoKonfigurasjon settOppKontoKonfigurasjon(FiksIOKlientProperties klientProperties, PrivateKey privateKey) {
        return KontoKonfigurasjon.builder().kontoId(klientProperties.kontoId()).privatNokkel(privateKey).build();
    }

    private static Maskinportenklient getMaskinportenKlient(MaskinportenProperties maskinportenProperties, FiksIOKlientProperties fiksIOKlientProperties) {
        MaskinportenklientProperties maskinportenklientProperties = MaskinportenklientProperties.builder()
            .audience(maskinportenProperties.audience())
            .issuer(maskinportenProperties.klientId().toString())
            .tokenEndpoint(maskinportenProperties.tokenEndpoint())
            .build();

        try {
            final var virksomhetssertifikatKonfigurasjon = settOppVirksomhetssertifikatKonfigurasjon(fiksIOKlientProperties);
            final var keyStore = virksomhetssertifikatKonfigurasjon.getKeyStore();
            final var keyAlias = virksomhetssertifikatKonfigurasjon.getKeyAlias();
            final var keyPassword = virksomhetssertifikatKonfigurasjon.getKeyPassword().toCharArray();

            return Maskinportenklient.builder()
                .withPrivateKey((PrivateKey) keyStore.getKey(keyAlias, keyPassword))
                .usingVirksomhetssertifikat((X509Certificate) keyStore.getCertificate(keyAlias))
                .withProperties(maskinportenklientProperties)
                .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static FiksIOKonfigurasjon settOppFiksIOKonfigurasjon(FiksIOKlientProperties klientProperties, MaskinportenProperties maskinportenProperties, FiksApiProperties fiksApiProperties, AmqpProperties amqpProperties, KontoKonfigurasjon kontoKonfigurasjon, VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon) {
        return FiksIOKonfigurasjon.builder()
            .fiksApiKonfigurasjon(FiksApiKonfigurasjon.builder()
                .host(fiksApiProperties.host())
                .port(fiksApiProperties.port())
                .scheme(fiksApiProperties.scheme())
                .build())
            .amqpKonfigurasjon(AmqpKonfigurasjon.builder()
                .host(amqpProperties.host())
                .port(amqpProperties.port())
                .applikasjonNavn(amqpProperties.applikasjonNavn())
                .mottakBufferStorrelse(amqpProperties.mottakBufferStorrelse())
                .build())
            .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                .idPortenKonfigurasjon(IdPortenKonfigurasjon.builder()
                    .accessTokenUri(maskinportenProperties.tokenEndpoint())
                    .idPortenAudience(maskinportenProperties.audience())
                    .klientId(maskinportenProperties.klientId().toString())
                    .build())
                .integrasjonId(klientProperties.integrasjonId())
                .integrasjonPassord(klientProperties.integrasjonPassword())
                .build())
            .kontoKonfigurasjon(kontoKonfigurasjon)
            .virksomhetssertifikatKonfigurasjon(virksomhetssertifikatKonfigurasjon)
            .build();
    }

    private static PrivateKey setOppPrivateKey(FiksIOKlientProperties klientProperties) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        try(var fileReader = new FileReader(fileFromResource(klientProperties.privatekeyFile()))) {
            try(var pemParser = new PEMParser(fileReader)) {
                return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(((PrivateKeyInfo) pemParser.readObject()).getEncoded()));
            }
        }
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
