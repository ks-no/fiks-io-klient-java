/*
package no.ks.fiks.svarinn.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.amqp.MeldingsType;
import no.ks.fiks.amqp.RabbitMqHeaders;
import no.ks.fiks.klient.svarinn2.api.v1.SvarInnApi;
import no.ks.fiks.klient.svarinn2.model.v1.*;
import no.ks.fiks.svarinn.client.model.Kvittering;
import no.ks.fiks.svarinn.client.model.MeldingSpesifikasjon;
import no.ks.fiks.svarinn2.model.MeldingsType;
import no.ks.fiks.svarinn2.model.RabbitMqHeaders;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
public class SvarInn {
    private ConnectionFactory factory;
    private SvarInnApi svarInnApi;
    private Connection connection;
    private Channel channel;


    public SvarInn(SvarInn2Settings settings) {
        this.factory = settings.getRabbitMqConnectionFactory();
        this.svarInnApi = settings.getSvarInn2Api();
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.confirmSelect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UUID sendMelding(UUID mottakerid, UUID svarPaMelding, byte[] message, UUID avsenderId) throws IOException {
        return sendMelding(mottakerid, svarPaMelding, message, avsenderId, (long) (60 * 60 * 2 * 24));
    }

    public UUID sendMelding(UUID mottakerid, byte[] message, UUID avsenderId) throws IOException {
        return sendMelding(mottakerid, null, message, avsenderId);
    }

    public UUID sendMelding(UUID mottakerid, UUID svarPaMelding, byte[] message, UUID avsenderId, Long ttl) throws IOException {
        final File pdf = File.createTempFile(UUID.randomUUID().toString(), "pdf");
        IOUtils.write(message, new FileOutputStream(pdf));
        final MeldingRespons melding = svarInnApi.sendMelding(avsenderId.toString(), mottakerid.toString(), "meldingstype", ttl, pdf, svarPaMelding != null ? svarPaMelding.toString() : null);
        return melding.getMeldingId();
    }

    public void sendKvitteringMottatt(UUID meldingId, UUID avsenderId, UUID kvitteringsMottakerId) {
        final MottattKvittering kvittering = new MottattKvittering()
                .kvitteringForMeldingId(meldingId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId);
        svarInnApi.kvitterMottatt(kvittering);
    }

    public void sendKvitteringBadRequest(UUID correlationId, UUID avsenderId, UUID kvitteringsMottakerId, String feilid, String melding) {
        final BadRequestKvittering kvittering = new BadRequestKvittering()
                .kvitteringForMeldingId(correlationId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId)
                .feilid(feilid)
                .melding(melding);
        svarInnApi.kvitterBadRequest(kvittering);
    }

    public void sendKvitteringFeilet(UUID correlationId, UUID avsenderId, UUID kvitteringsMottakerId, String feilid, String melding) {
        final FeiletKvittering kvittering = new FeiletKvittering()
                .kvitteringForMeldingId(correlationId)
                .avsenderId(avsenderId)
                .kvitteringsMottakerId(kvitteringsMottakerId)
                .feilid(feilid)
                .melding(melding);
        svarInnApi.kvitterFeilet(kvittering);
    }

    public void consume(UUID mottakerid, final SvarInnMeldingConsumer consumer, final SvarInnKvitteringConsumer kvitteringConsumer) {
        try {
            channel.basicConsume(mottakerid.toString(), new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    if (String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)).startsWith("kvittering.")) {
                        Object avsenderId = properties.getHeaders().get(RabbitMqHeaders.AVSENDER_ID);
                        log.info("Kvittering mottat, headers:" + properties.getHeaders());
                        final Kvittering kvittering = new Kvittering(
                                UUID.fromString(avsenderId.toString()),
                                UUID.fromString(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.KVITTERING_FOR_MELDING))),
                                String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)));
                        try {
                            if (MeldingsType.KVITTERING_BAD_REQUEST.equals(kvittering.getType())) {
                                kvittering.setBadRequestKvittering(new ObjectMapper().readValue(body, BadRequestKvittering.class));
                            } else if (MeldingsType.KVITTERING_FEILET.equals(kvittering.getType())) {
                                kvittering.setFeiletKvittering(new ObjectMapper().readValue(body, FeiletKvittering.class));
                            } else if (MeldingsType.KVITTERING_MOTTATT.equals(kvittering.getType())) {
                                kvittering.setMottattKvittering(new ObjectMapper().readValue(body, MottattKvittering.class));
                            } else if (MeldingsType.KVITTERING_TIMEOUT.equals(kvittering.getType())) {
                                kvittering.setExpiredKvittering(new ObjectMapper().readValue(body, ExpiredKvittering.class));
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        kvitteringConsumer.handleKvittering(kvittering);
                        try {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (IOException e) {
                            throw new RuntimeException("", e);
                        }
                    } else {

                        final MeldingSpesifikasjon melding = MeldingSpesifikasjon.builder()
                                .meldingId(UUID.fromString(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_ID))))
                                .avsenderId(UUID.fromString(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.AVSENDER_ID))))
                                .meldingType(String.valueOf(properties.getHeaders().get(RabbitMqHeaders.MELDING_TYPE)))
                                .svarPaMelding(Optional.fromNullable(properties.getHeaders().get(RabbitMqHeaders.SVAR_PA_MELDING_ID)).transform(v -> UUID.fromString(String.valueOf(v))).orNull())
                                .melding(body).build();

                        consumer.prosesserMelding(melding);
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

    public MeldingId send(MeldingSpesifikasjon meldingSpesifikasjon) {
    }

    public void subscribe(SubscribeSettings subscribeSettings) {

    }

    public void lookup() {

    }

    public KontoId lookup(String s, String s1, int i) {

    }
}
*/
