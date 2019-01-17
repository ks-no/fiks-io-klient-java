package no.ks.fiks.svarinn2.klient.java;

import no.ks.fiks.componenttest.support.ComponentTestConfigurationProperties;
import no.ks.fiks.componenttest.support.feign.TestApiBuilder;
import no.ks.fiks.konfigurasjon.mock.KonfigurasjonMock;
import no.ks.fiks.svarinn.client.SvarInnKlientImpl;
import no.ks.fiks.svarinn.client.konfigurasjon.*;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKontoApi;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Konto;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.KontoSpesifikasjon;
import no.ks.fiks.test.docker.DockerComposeIpResolver;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.File;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;

class SvarInn2KlientGenerator {
    private final SvarInnKontoApi svarInnKontoApi;
    private ComponentTestConfigurationProperties properties;
    private final KonfigurasjonMock konfigurasjonMock;

    SvarInn2KlientGenerator(ComponentTestConfigurationProperties properties,
                            KonfigurasjonMock konfigurasjonMock,
                            TestApiBuilder<SvarInnKontoApi> kontoApiBuilder) {
        this.properties = properties;
        this.konfigurasjonMock = konfigurasjonMock;
        this.svarInnKontoApi = kontoApiBuilder.asPerson(TestUtil.randomPerson()).build();
    }

    SvarInnKlientImpl opprettKontoOgKlient(KeyStore keyStore, String keyStorePassword, String certAlias, String keyAlias, String keyPassword) throws Exception {
        UUID integrasjonId = UUID.randomUUID();
        String password = UUID.randomUUID().toString();

        X509Certificate cert = (X509Certificate) keyStore.getCertificate(certAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());

        konfigurasjonMock.setupIntegrasjonAuthenticateOk(integrasjonId);

        Konto opprettetKonto = svarInnKontoApi.opprettKonto(new KontoSpesifikasjon()
                .fiksOrgId(UUID.randomUUID())
                .navn(UUID.randomUUID().toString()));

        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(cert);
        jcaPEMWriter.flush();
        String s = stringWriter.toString();

        File certFile = File.createTempFile(opprettetKonto.getKontoId().toString(), "cert");
        FileUtils.writeStringToFile(certFile, s, "UTF-8");

        svarInnKontoApi.settOffentligNokkelPem(opprettetKonto.getKontoId(), certFile);

        svarInnKontoApi.aktiver(opprettetKonto.getKontoId());

        return new SvarInnKlientImpl(SvarInnKonfigurasjon.builder()
                .fiksApiKonfigurasjon(FiksApiKonfigurasjon.builder()
                        .scheme("http")
                        .port(8080)
                        .build())
                .sendMeldingKonfigurasjon(SendMeldingKonfigurasjon.builder()
                        .host(DockerComposeIpResolver.getIp(properties.getProject(), "fiks-svarinn2-service").orElseThrow(() -> new RuntimeException("Could not find svarinn-service")))
                        .build())
                .katalogKonfigurasjon(KatalogKonfigurasjon.builder()
                        .host(DockerComposeIpResolver.getIp(properties.getProject(), "fiks-svarinn2-katalog-service").orElseThrow(() -> new RuntimeException("Could not find svarinn-service")))
                        .build())
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                        .kontoId(new KontoId(opprettetKonto.getKontoId()))
                        .privatNokkel(privateKey)
                        .build())
                .signeringKonfigurasjon(SigneringKonfigurasjon.builder()
                        .keyAlias(keyAlias)
                        .keyPassword(keyPassword)
                        .keyStore(keyStore)
                        .keyStorePassword(keyStorePassword)
                        .build())
                .amqpKonfigurasjon(AmqpKonfigurasjon.builder()
                        .host(DockerComposeIpResolver.getIp(properties.getProject(), "rabbitmq").orElseThrow(() -> new RuntimeException("Could not find rabbitmq container")))
                        .port(5672)
                        .build())
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(integrasjonId)
                        .integrasjonPassord(password)
                        .idPortenKonfigurasjon(IdPortenKonfigurasjon.builder()
                                .virksomhetsertifikat(cert)
                                .privatNokkel(privateKey)
                                .klientId("asdf")
                                .accessTokenUri("http://" + DockerComposeIpResolver.getIp(properties.getProject(), "oidc-mock").orElseThrow(() -> new RuntimeException("Could not find oidc mock container")) + ":8080/oidc-provider-mock/token")
                                .idPortenAudience("asdf")
                                .build())
                        .build())
                .build());
    }
}