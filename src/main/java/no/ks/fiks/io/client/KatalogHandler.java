package no.ks.fiks.io.client;

import feign.FeignException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.LookupRequest;
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;


@Slf4j
public class KatalogHandler {
    private final FiksIoKatalogApi katalogApi;
    private final FiksIoKatalogApi publicKatalogApi;
    private CertificateFactory cf;

    public KatalogHandler(@NonNull FiksIoKatalogApi katalogApiAuth, @NonNull FiksIoKatalogApi publicKatalogApi) {
        this.katalogApi = katalogApiAuth;
        this.publicKatalogApi = publicKatalogApi;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        try {
            return Optional.ofNullable(katalogApi.lookup(
                request.getIdentifikator()
                    .getIdentifikatorType()
                    .name() + "." + request.getIdentifikator()
                    .getIdentifikator(),
                request.getMeldingsprotokoll(), request.getSikkerhetsNiva()))
                .map(Konto::fromKatalogModel);
        } catch (FeignException e) {
            if (e.status() == 404)
                return Optional.empty();
            else
                throw e;
        }
    }

    public Optional<Konto> getKonto(@NonNull KontoId kontoId) {
        try {
            return Optional.ofNullable(katalogApi.getKonto(kontoId.getUuid())).map(Konto::fromKatalogModel);
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

            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(publicKatalogApi.getOffentligNokkel(mottakerKontoId.getUuid()).getNokkel().getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }
}
