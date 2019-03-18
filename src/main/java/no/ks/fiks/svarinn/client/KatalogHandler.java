package no.ks.fiks.svarinn.client;

import feign.FeignException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.svarinn.client.api.katalog.api.SvarInnKatalogApi;
import no.ks.fiks.svarinn.client.api.katalog.model.OffentligNokkel;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.KontoId;
import no.ks.fiks.svarinn.client.model.LookupRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

@Slf4j
public class KatalogHandler {
    private SvarInnKatalogApi katalogApi;

    private CertificateFactory cf;

    public KatalogHandler(@NonNull SvarInnKatalogApi katalogApi) {
        this.katalogApi = katalogApi;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        try {
            return Optional.ofNullable(katalogApi.lookup(
                request.getIdentifikator()
                       .getIdentifikatorType()
                       .name() + "." + request.getIdentifikator()
                                              .getIdentifikator(),
                request.getMeldingType(), request.getSikkerhetsNiva()))
                           .map(Konto::fromKatalogModel);
        } catch (FeignException e) {
            if (e.status() == 404) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    X509Certificate getPublicKey(@NonNull KontoId mottakerKontoId) {
        try {
            if (cf == null) {
                cf = CertificateFactory.getInstance("X.509");
            }

            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(getOffentligNokkel(mottakerKontoId).getNokkel()
                                                                                                                        .getBytes(StandardCharsets.UTF_8)));
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }

    private OffentligNokkel getOffentligNokkel(@NonNull final KontoId mottakerKontoId) {
        log.debug("Henter nøkkel for konto \"{}\"", mottakerKontoId);
        return katalogApi.getOffentligNokkel(mottakerKontoId.getUuid());
    }
}
