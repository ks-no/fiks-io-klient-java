package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import feign.FeignException;
import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.MeldingRequest;
import no.ks.fiks.svarinn.client.model.MottattMelding;
import no.ks.fiks.svarinn.client.model.SendtMelding;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.model.MottattMeldingMetadata;
import no.ks.fiks.svarinn2.model.SvarInnMeldingParser;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class SvarInn2 {

    private final SvarInnApi svarInnApi;
    private final KontoId kontoId;
    private final SvarInnKatalogApi katalogApi;
    private final AsicHandler asic;
    private CertificateFactory cf;

    public SvarInn2(@NonNull SvarInnKonfigurasjon settings) {
        svarInnApi = settings.getSvarInn2Api();
        katalogApi = settings.getKatalogApi();
        kontoId = settings.getKontoKonfigurasjon().getKontoId();
        asic = new AsicHandler(settings.getKontoKonfigurasjon().getPrivatNokkel());
    }

    public KontoId getKontoId(){
        return kontoId;
    }

    public Optional<Konto> lookup(@NonNull LookupRequest request) {
        try{
            return Optional.of(Konto.fromKatalogModel(katalogApi.lookup(request.getIdentifikator().getIdentifikatorType().getValue(), request.getIdentifikator().getIdentifikator(), request.getDokumentType(), request.getSikkerhetsNiva())));
        } catch (FeignException e){
            if (e.status() == 404)
                return Optional.empty();
            else
                throw e;
        }
    }

    public SendtMelding send(MeldingRequest request, InputStream payload) {
        return SendtMelding.fromSendResponse(svarInnApi.sendMelding(
                kontoId.toString(),
                request.getMottakerKontoId().toString(),
                request.getMeldingType(),
                asic.encrypt(getPublicKey(request.getMottakerKontoId()), payload),
                Option.of(request.getSvarPaMelding()).map(MeldingId::toString).getOrElse(() -> null),
                request.getTtl().toMillis()));
    }

    public SendtMelding send(MeldingRequest request, String payload) {
        return send(request, new ByteArrayInputStream(payload.getBytes()));
    }

    public SendtMelding send(MeldingRequest request, File payload) throws FileNotFoundException {
        return send(request, new FileInputStream(payload));
    }

    public void subscribe(Channel channel, SubscribeSettings subscribe) {
        try {
            channel.basicConsume(kontoId.toString(), (ct, m) -> {
                MottattMeldingMetadata parsed = SvarInnMeldingParser.parse(m.getEnvelope(), m.getProperties());

                MottattMelding melding = MottattMelding.fromMottattMeldingMetadata(parsed, asic.decrypt(m.getBody()));
                subscribe.getOnMelding().accept(melding, KvitteringSender.builder()
                        .mottakerSertifikat(getPublicKey(melding.getAvsenderKontoId()))
                        .channel(channel)
                        .svarInnApi(svarInnApi)
                        .deliveryTag(parsed.getDeliveryTag())
                        .meldingSomSkalKvitteres(melding)
                        .asicGenerator(asic)
                        .build());
            }, (consumerTag, sig) -> subscribe.getOnClose().accept(sig));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private X509Certificate getPublicKey(KontoId mottakerKontoId) {
        String offentligNokkelPem = katalogApi.getOffentligNokkelPem(mottakerKontoId.getUuid());

        try {
            if (cf == null)
                cf = CertificateFactory.getInstance("X.509");

            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(offentligNokkelPem.getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }
}
