package no.ks.fiks.svarinn.client;

import feign.FeignException;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.api.katalog.api.SvarInnKatalogApi;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class KatalogHandler {
    private SvarInnKatalogApi katalogApi;
    private CertificateFactory cf;

    public KatalogHandler(SvarInnKatalogApi katalogApi) {
        this.katalogApi = katalogApi;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        try {
            return Optional.of(Konto.fromKatalogModel(katalogApi.lookup(
                request.getIdentifikator().getIdentifikatorType().name() + "." + request.getIdentifikator()
                                                                                            .getIdentifikator(),
                request.getMeldingType(), request.getSikkerhetsNiva())));
        } catch (FeignException e) {
            if (e.status() == 404)
                return Optional.empty();
            else
                throw e;
        }
    }

    X509Certificate getPublicKey(@NonNull KontoId mottakerKontoId) {
        try {
            if (cf == null)
                cf = CertificateFactory.getInstance("X.509");

            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(katalogApi.getOffentligNokkel(mottakerKontoId.getUuid()).getNokkel().getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }
}
