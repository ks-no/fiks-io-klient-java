package no.ks.fiks.io.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaTest {
    @Test
    void testLastVersjonnummer() {
        assertNotNull(Meta.VERSJON);
    }
}