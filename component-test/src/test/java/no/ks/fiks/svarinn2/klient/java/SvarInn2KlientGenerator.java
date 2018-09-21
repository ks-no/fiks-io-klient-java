package no.ks.fiks.svarinn2.klient.java;

import com.rabbitmq.client.ConnectionFactory;
import io.vavr.Tuple2;
import no.ks.fiks.componenttest.support.ComponentTestConfigurationProperties;
import no.ks.fiks.componenttest.support.feign.TestApiBuilder;
import no.ks.fiks.componenttest.support.konfigurasjon.KonfigurasjonMock;
import no.ks.fiks.svarinn.client.*;
import no.ks.fiks.svarinn.client.model.FiksIntegrasjonKonfigurasjon;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.KontoKonfigurasjon;
import no.ks.fiks.svarinn.client.model.SvarInnKonfigurasjon;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKontoApi;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.Konto;
import no.ks.fiks.svarinn2.katalog.swagger.model.v1.KontoSpesifikasjon;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;
import no.ks.fiks.test.docker.DockerComposeIpResolver;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.File;
import java.io.StringWriter;
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
                                   KonfigurasjonMock konfigurasjonMock,
                                   TestApiBuilder<SvarInnKontoApi> kontoApiBuilder,
                                   TestApiBuilder<SvarInnKatalogApi> katalogApiBuilder,
                                   TestApiBuilder<SvarInnApi> svarInnApiBuilder) {
        this.properties = properties;
        this.konfigurasjonMock = konfigurasjonMock;

        this.svarInnApi = svarInnApiBuilder.asPerson(TestUtil.randomFnr()).build();
        this.svarInnKontoApi = kontoApiBuilder.asPerson(TestUtil.randomFnr()).build();
        this.katalogApi = katalogApiBuilder.asPerson(TestUtil.randomFnr()).build();
    }

    public SvarInnKlient opprettKontoOgKlient(Tuple2<X509Certificate, PrivateKey> credentials, ConnectionFactory factory) throws Exception {
        UUID integrasjonId = UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();

        konfigurasjonMock.setupIntegrasjonAuthenticateOk(integrasjonId, password, token);

        Konto opprettetKonto = svarInnKontoApi.opprettKonto(new KontoSpesifikasjon()
                .fiksOrgId(UUID.randomUUID())
                .navn(UUID.randomUUID().toString()));

        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(credentials._1);
        jcaPEMWriter.flush();
        String s = stringWriter.toString();

        File certFile = File.createTempFile(opprettetKonto.getKontoId().toString(), "cert");
        FileUtils.writeStringToFile(certFile, s, "UTF-8");

        svarInnKontoApi.settOffentligNokkelPem(opprettetKonto.getKontoId(), certFile);

        factory.setHost(DockerComposeIpResolver.getIp(properties.getProject(), "rabbitmq").orElseThrow(() -> new RuntimeException("Could not find rabbitmq container")));
        factory.setPort(5672);
        factory.setUsername(integrasjonId.toString());
        factory.setPassword(String.format("%s %s", password, token));

        return new SvarInnKlient(SvarInnKonfigurasjon.builder()
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                        .kontoId(new KontoId(opprettetKonto.getKontoId()))
                        .privatNokkel(credentials._2)
                        .build())
                .lookupCache(p -> new KontoId(UUID.randomUUID()))
                .svarInn2Api(svarInnApi)
                .katalogApi(katalogApi)
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(integrasjonId)
                        .integrasjonPassord(password)
                        .virksomhetsertifikat(credentials._1)
                        .build())
                .build());
    }
}
