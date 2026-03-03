package no.ks.fiks.io.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.commons.MottattMeldingMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class AmqpHandlerTest {

    @DisplayName("isHarPayload settes til false, dersom Delivery ikke inneholder data")
    @Test
    void fraDeliveryUtenPayload() {
        AMQP.BasicProperties basicProperties = Mockito.mock(AMQP.BasicProperties.class);
        when(basicProperties.getHeaders()).thenReturn(new HashMap<>());
        Delivery delivery = new Delivery(new Envelope(1L, true, "exchange", "routingKey"), basicProperties, new byte[0] );
        AsicHandler asicHandler = Mockito.mock(AsicHandler.class);
        doNothing().when(asicHandler).writeDecrypted(any(),any());
        DokumentlagerKlient dokumentlagerKlient = Mockito.mock(DokumentlagerKlient.class);
        MottattMeldingMetadata.MottattMeldingMetadataBuilder mottattMeldingMetadataBuilder = MottattMeldingMetadata.builder();
        mottattMeldingMetadataBuilder.meldingType("meldingstype").meldingId(UUID.randomUUID()).ttl(1L).deliveryTag(1L).avsenderKontoId(UUID.randomUUID()).mottakerKontoId(UUID.randomUUID());

        MottattMelding melding = AmqpHandler.getMelding(delivery, mottattMeldingMetadataBuilder.build(), asicHandler, dokumentlagerKlient);

        assertFalse(melding.isHarPayload());
    }
}
