package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import feign.FeignException;
import lombok.NonNull;
import no.ks.fiks.svarinn.client.model.Konto;
import no.ks.fiks.svarinn.client.model.Melding;
import no.ks.fiks.svarinn.client.model.MeldingRequest;
import no.ks.fiks.svarinn2.katalog.swagger.api.v1.SvarInnKatalogApi;
import no.ks.fiks.svarinn2.model.MottattMelding;
import no.ks.fiks.svarinn2.model.SvarInnMeldingParser;
import no.ks.fiks.svarinn2.swagger.api.v1.SvarInnApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SvarInn2 {

    private final SvarInnApi svarInnApi;
    private final Channel channel;
    private final KontoId kontoId;
    private final SvarInnKatalogApi katalogApi;
    private final AsicGenerator asicGenerator;

    public SvarInn2(@NonNull SvarInn2Settings settings) {
        svarInnApi = settings.getSvarInn2Api();
        katalogApi = settings.getKatalogApi();

        channel = settings.getRabbitMqChannel();
        kontoId = settings.getKontoId();

        asicGenerator = new AsicGenerator(settings.getSertifikat(), settings.getPrivatNokkel());
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

    /**
     *
     * Sender en melding til en spesifisert SvarInn2 konto.
     *
     * @param request spesifikasjon av meldingen som skal sendes
     * @return den sendte meldingen
     * @throws UgyldigMottakerKontoException hvis mottakerkonto er deaktivert, mottaker konto er ukjent, eller hvis mottakerkonto ikke støtter den spesiserte dokumenttypen.

     */
    public Melding send(MeldingRequest request, InputStream payload) {
        return Melding.fromSendResponse(svarInnApi.sendMelding(kontoId.toString(), request.getMottakerKontoId().toString(), request.getMeldingType(), asicGenerator.encrypt(payload), request.getSvarPaMelding().toString(), request.getTtl().toMillis()));
    }

    public Melding send(MeldingRequest request, String payload) {
        return Melding.fromSendResponse(svarInnApi.sendMelding(kontoId.toString(), request.getMottakerKontoId().toString(), request.getMeldingType(), asicGenerator.encrypt(payload), request.getSvarPaMelding().toString(), request.getTtl().toMillis()));
    }

    public Melding send(MeldingRequest request, File payload) {
        return Melding.fromSendResponse(svarInnApi.sendMelding(kontoId.toString(), request.getMottakerKontoId().toString(), request.getMeldingType(), asicGenerator.encrypt(payload), request.getSvarPaMelding().toString(), request.getTtl().toMillis()));
    }

    public void subscribe(SubscribeSettings subscribe) {
        try {
            channel.basicConsume(kontoId.toString(), new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    MottattMelding parse = SvarInnMeldingParser.parse(envelope, properties);

                    subscribe.getOnMelding().accept(Melding.fromMottattMelding(parse), KvitteringSender.builder()
                            .channel(channel)
                            .svarInnApi(svarInnApi)
                            .deliveryTag(parse.getDeliveryTag()).meldingSomSkalKvitteres(Melding.fromMottattMelding(parse))
                            .asicGenerator(asicGenerator)
                            .build());

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
