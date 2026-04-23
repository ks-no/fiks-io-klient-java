package no.ks.fiks.io.client;

import feign.FeignException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.fiksio.client.api.katalog.api.FiksIoKatalogApi;
import no.ks.fiks.fiksio.client.api.konfigurasjon.api.FiksIoKontoApi;
import no.ks.fiks.fiksio.client.api.konfigurasjon.model.OppdaterOffentligNokkelSpesifikasjon;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;


@Slf4j
public class KatalogHandler {
    private final FiksIoKatalogApi katalogApi;
    private final FiksIoKatalogApi publicKatalogApi;
    private final FiksIoKontoApi kontoApi;

    private CertificateFactory cf;

    public KatalogHandler(@NonNull FiksIoKatalogApi katalogApiAuth, @NonNull FiksIoKatalogApi publicKatalogApi, FiksIoKontoApi kontoApi) {
        this.katalogApi = katalogApiAuth;
        this.publicKatalogApi = publicKatalogApi;
        this.kontoApi = kontoApi;
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

    public Boolean hasPublicKey(@NonNull KontoId kontoId) {
        try {
            return getPublicKeyFromKatalogApi(kontoId) != null;
        } catch (FeignException.NotFound | CertificateException exception) {
            return false;
        }
    }

    public void uploadPublicKey(@NonNull KontoId kontoId, @NonNull String publicKey) {
        requireFiksIoKontoApi();

        kontoApi.settOffentligNokkel(kontoId.getUuid(), new OppdaterOffentligNokkelSpesifikasjon().pem(publicKey));
    }

    private void requireFiksIoKontoApi() {
        if(kontoApi == null) {
            throw new RuntimeException("Kan ikke laste opp public key grunnet manglene FiksIOKontoApi klient");
        }
    }

    X509Certificate getPublicKey(@NonNull KontoId mottakerKontoId) {
        try {
            setCertificateFactoryIfNull();
            return getPublicKeyFromKatalogApi(mottakerKontoId);
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }

    private X509Certificate getPublicKeyFromKatalogApi(@NonNull KontoId mottakerKontoId) throws CertificateException {
        setCertificateFactoryIfNull();
        try {
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(publicKatalogApi.getOffentligNokkel(mottakerKontoId.getUuid()).getNokkel().getBytes()));
        } catch (FeignException.NotFound e) {
            return null;
        }

    }

    private void setCertificateFactoryIfNull() throws CertificateException {
        if (cf == null)  {
            cf = CertificateFactory.getInstance("X.509");
        }
    }
}
