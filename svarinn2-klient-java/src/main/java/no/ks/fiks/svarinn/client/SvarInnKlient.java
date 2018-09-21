package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import feign.FeignException;
import io.vavr.control.Option;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.model.*;
import no.ks.fiks.svarinn2.commons.MottattMeldingMetadata;
import no.ks.fiks.svarinn2.commons.SvarInnMeldingParser;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class SvarInnKlient {

    private final SvarInnApi svarInnApi;
    private final KontoId kontoId;
    private final SvarInnKatalogApi katalogApi;
    private final AsicHandler asic;
    private CertificateFactory cf;

    public SvarInnKlient(@NonNull SvarInnKonfigurasjon settings) {
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

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull List<Payload> payload) {
        return SendtMelding.fromSendResponse(svarInnApi.sendMelding(
                kontoId.toString(),
                request.getMottakerKontoId().toString(),
                request.getMeldingType(),
                asic.encrypt(getPublicKey(request.getMottakerKontoId()), payload),
                Option.of(request.getSvarPaMelding()).map(MeldingId::toString).getOrElse(() -> null),
                request.getTtl().toMillis()));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull String payload) {
        return send(request, singletonList(new StringPayload(payload, "payload.txt")));
    }

    public SendtMelding send(@NonNull MeldingRequest request, @NonNull File payload) {
        return send(request, singletonList(new FilePayload(payload)));
    }

    public void subscribe(@NonNull Channel channel, @NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding) {
        subscribe(channel, onMelding, p -> {});
    }

    public void subscribe(@NonNull Channel channel, @NonNull BiConsumer<MottattMelding, KvitteringSender> onMelding, @NonNull Consumer<ShutdownSignalException> onClose) {
        try {
            channel.basicConsume(kontoId.toString(), (ct, m) -> {
                MottattMeldingMetadata parsed = SvarInnMeldingParser.parse(m.getEnvelope(), m.getProperties());

                MottattMelding melding = MottattMelding.fromMottattMeldingMetadata(parsed, asic.decrypt(m.getBody()));
                onMelding.accept(melding, KvitteringSender.builder()
                        .mottakerSertifikat(getPublicKey(melding.getAvsenderKontoId()))
                        .channel(channel)
                        .svarInnApi(svarInnApi)
                        .deliveryTag(parsed.getDeliveryTag())
                        .meldingSomSkalKvitteres(melding)
                        .asicGenerator(asic)
                        .build());
            }, (consumerTag, sig) -> onClose.accept(sig));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private X509Certificate getPublicKey(@NonNull KontoId mottakerKontoId) {
        try {
            if (cf == null)
                cf = CertificateFactory.getInstance("X.509");

            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(katalogApi.getOffentligNokkelPem(mottakerKontoId.getUuid()).getNokkel().getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException(String.format("Feil under generering av offentlig sertifikat for mottaker %s", mottakerKontoId), e);
        }
    }
}
