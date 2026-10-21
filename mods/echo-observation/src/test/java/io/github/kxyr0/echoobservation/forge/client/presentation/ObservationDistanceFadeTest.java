package io.github.kxyr0.echoobservation.forge.client.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationDistanceFadeTest {
    @Test
    void eightBlockObservationFadesAcrossItsLastTwoBlocks() {
        assertEquals(1.0D, ObservationDistanceFade.opacity(6.0D, 8.0D), 0.000_001D);
        double middle = ObservationDistanceFade.opacity(7.0D, 8.0D);
        assertTrue(middle > 0.0D && middle < 1.0D);
        assertEquals(0.0D, ObservationDistanceFade.opacity(8.0D, 8.0D), 0.000_001D);
    }
}
