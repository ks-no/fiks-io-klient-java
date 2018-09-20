package no.ks.fiks.svarinn2.klient.java;

import com.rabbitmq.client.ConnectionFactory;
import no.ks.fiks.commons.authorization.Privilegier;
import no.ks.fiks.commons.authorization.RessursType;
import no.ks.fiks.componenttest.support.ComponentTestConfigurationProperties;
import no.ks.fiks.componenttest.support.autorisasjon.AutorisasjonQueryMock;
import no.ks.fiks.componenttest.support.autorisasjon.AutorisasjonUpdateMock;
import no.ks.fiks.componenttest.support.feign.TestApiBuilder;
import no.ks.fiks.componenttest.support.konfigurasjon.KonfigurasjonMock;
import no.ks.fiks.svarinn.client.*;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKontoApi;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Konto;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.KontoSpesifikasjon;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.test.docker.DockerComposeIpResolver;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class SvarInn2KlientGenerator {
    private final SvarInnApi svarInnApi;
    private final SvarInnKontoApi svarInnKontoApi;
    private final SvarInnKatalogApi katalogApi;
    private ComponentTestConfigurationProperties properties;
    private final KonfigurasjonMock konfigurasjonMock;

    public SvarInn2KlientGenerator(ComponentTestConfigurationProperties properties,
                                   AutorisasjonQueryMock autorisasjonQueryMock,
                                   KonfigurasjonMock konfigurasjonMock,
                                   AutorisasjonUpdateMock autorisasjonUpdateMock,
                                   TestApiBuilder<SvarInnKontoApi> kontoApiBuilder,
                                   TestApiBuilder<SvarInnKatalogApi> katalogApiBuilder,
                                   TestApiBuilder<SvarInnApi> svarInnApiBuilder) {
        this.properties = properties;
        this.konfigurasjonMock = konfigurasjonMock;

        this.svarInnApi = svarInnApiBuilder.asPerson(TestUtil.randomFnr()).build();
        this.svarInnKontoApi = kontoApiBuilder.asPerson(TestUtil.randomFnr()).build();
        this.katalogApi = katalogApiBuilder.asPerson(TestUtil.randomFnr()).build();

        autorisasjonQueryMock.setupAuthorizationQuery(null, Privilegier.ADMIN, null, RessursType.TJENESTE_SVARINN.getId());
        autorisasjonQueryMock.setupAuthorizationSuccess();
        autorisasjonQueryMock.setupRessurs();
        autorisasjonUpdateMock.setupRessursCreation();
    }

    public SvarInn2 opprettKontoOgKlient(InputStream jksStream, ConnectionFactory factory) throws Exception {
        UUID integrasjonId = UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();

        konfigurasjonMock.setupIntegrasjonAuthenticateOk(integrasjonId, password, token);

        Konto opprettetKonto = svarInnKontoApi.opprettKonto(new KontoSpesifikasjon()
                .fiksOrgId(UUID.randomUUID())
                .navn(UUID.randomUUID().toString()));

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(jksStream, "123456".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("kommune1keyalias");

        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(cert);
        jcaPEMWriter.flush();
        String s = stringWriter.toString();


        File certFile = File.createTempFile(opprettetKonto.getKontoId().toString(), "cert");
        FileUtils.writeStringToFile(certFile, s, "UTF-8");




        svarInnKontoApi.settOffentligNokkelPem(opprettetKonto.getKontoId(), certFile);

        factory.setHost(DockerComposeIpResolver.getIp(properties.getProject(), "rabbitmq").orElseThrow(() -> new RuntimeException("Could not find rabbitmq container")));
        factory.setPort(5672);
        factory.setUsername(integrasjonId.toString());
        factory.setPassword(String.format("%s %s", password, token));




        System.out.println(keyStore.aliases().nextElement());
        // Fetching certificate

        return new SvarInn2(SvarInnKonfigurasjon.builder()
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                        .kontoId(new KontoId(opprettetKonto.getKontoId()))
                        .privatNokkel((PrivateKey) keyStore.getKey("kommune1keyalias", "123456".toCharArray()))
                        .build())
                .lookupCache(p -> new KontoId(UUID.randomUUID()))
                .svarInn2Api(svarInnApi)
                .katalogApi(katalogApi)
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(integrasjonId)
                        .integrasjonPassord(password)
                        .virksomhetsertifikat(cert)
                        .build())
                .build());

    }
}
