package no.ks.fiks.svarinn.client.konfigurasjon;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import no.ks.fiks.svarinn.client.model.MeldingId;

import java.util.function.Predicate;
/**
 * Konfigurer amqp-klienten som benyttes for å lytte på meldinger fra svarinn. Hvis denne ikke er konfigurert vil hosten konfigurert over benyttes sammen med default port.
 */
@Data
@Builder
public class AmqpKonfigurasjon {

    /**
     * Ikke påkrevd felt. Om feltet ikke er oppgitt benyttes fiksApi.host
     */
    private String host;

    /**
     * Ikke påkrevd. Hvis port ikke settes vil default amqp port 5672 benyttes.
     */
    @Builder.Default private Integer port = 5672;

    /**
     * Ikke påkrevd. Det er her mulig å konfigurere et predikat som forteller om en spesifikk melding har blitt behandlet tidligere, slik at man unngår duplikat meldinger som ellers kan oppstå gjennom en amqp kobling, pga. nettverksbrudd eller lignende.
     */
    @Builder.Default private Predicate<MeldingId> meldingErBehandlet = m -> true;
}
