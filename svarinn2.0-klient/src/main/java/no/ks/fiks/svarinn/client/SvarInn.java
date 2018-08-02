package no.ks.fiks.svarinn.client;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.amqp.RabbitMqHeaders;
import no.ks.fiks.klient.mottak.api.v1.SvarInnMeldingApi;
import no.ks.fiks.klient.mottak.model.v1.BadRequestKvittering;
import no.ks.fiks.klient.mottak.model.v1.FeiletKvittering;
import no.ks.fiks.klient.mottak.model.v1.Melding;
import no.ks.fiks.klient.mottak.model.v1.MottattKvittering;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Slf4j
public class SvarInn {
    private ConnectionFactory factory;
    private SvarInnMeldingApi svarInnMeldingApi;
    private Connection connection;
    private Channel channel;


    public SvarInn(ConnectionFactory factory, SvarInnMeldingApi svarInnMeldingApi) {
        this.factory = factory;
        this.svarInnMeldingApi = svarInnMeldingApi;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.confirmSelect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMelding(UUID mottakerid, UUID korrelasjonId, String message, UUID avsenderId) {
        sendMelding(mottakerid, korrelasjonId, message, avsenderId, null);
    }

    public void sendMelding(UUID mottakerid, UUID korrelasjonId, String message, UUID avsenderId, Long ttl) {
        final Melding body = new Melding()
                .contentType("application/json")
                .korrelasjonId(korrelasjonId)
                .meldingType("test.type")
                .melding(message)
                .avsenderId(avsenderId)
                .meldingsMottakerId(mottakerid)
                .ttl(ttl);

        svarInnMeldingApi.sendMelding( body);
    }

    public void sendKvitteringMottatt(UUID correlationId, UUID avsenderId, UUID kvitteringsMottakerId) {
        final MottattKvittering kvittering = new MottattKvittering()
                .korrelasjonId(correlationId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId);
        svarInnMeldingApi.kvitterMottatt(kvittering);

    }

    public void sendKvitteringBadRequest(UUID correlationId, UUID avsenderId, UUID kvitteringsMottakerId, String feilid, String melding) {
        final BadRequestKvittering kvittering = new BadRequestKvittering()
                .korrelasjonId(correlationId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId)
                .feilid(feilid)
                .melding(melding);
        svarInnMeldingApi.kvitterBadRequest(kvittering);

    }
    public void sendKvitteringFeilet(UUID correlationId, UUID avsenderId, UUID kvitteringsMottakerId, String feilid, String melding) {
        final FeiletKvittering kvittering = new FeiletKvittering()
                .korrelasjonId(correlationId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId)
                .feilid(feilid)
                .melding(melding);
        svarInnMeldingApi.kvitterFeilet(kvittering);

    }

    public void consume(UUID mottakerid, final SvarInnMessageConsumer consumer, final SvarInnKvitteringConsumer kvitteringConsumer) {
        try {
            channel.basicConsume(mottakerid.toString(), new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    if (String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)).startsWith("kvittering.")) {
                        Object avsenderId = properties.getHeaders().get(RabbitMqHeaders.AVSENDER_ID);

                        final Kvittering kvittering = new Kvittering(
                                UUID.fromString(avsenderId.toString()),
                                UUID.fromString(properties.getCorrelationId()),
                                String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)),
                                body);

                        kvitteringConsumer.handleKvittering(kvittering);
                        try {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (IOException e) {
                            throw new RuntimeException("", e);
                        }
                    } else {

                        final Melding melding = new Melding()
                                .korrelasjonId(UUID.fromString(properties.getCorrelationId()))
                                .avsenderId(UUID.fromString(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.AVSENDER_ID))))
                                .meldingType(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)));

                        try {
                            melding.setMelding(new String(body, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }

                        consumer.handleMessage(melding);
                        try {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (IOException e) {
                            throw new RuntimeException("", e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }


    public void close() throws IOException {
        try {
            channel.waitForConfirms();
            channel.close();
        } catch (Exception e) {
            log.warn("Got exception while closing melding channel", e);
        }

        connection.close();
    }
}
