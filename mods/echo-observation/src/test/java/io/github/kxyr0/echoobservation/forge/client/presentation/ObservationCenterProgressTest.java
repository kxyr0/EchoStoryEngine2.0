package io.github.kxyr0.echoobservation.forge.client.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationCenterProgressTest {
    @Test
    void radiusGrowsFromCenterAndReachesOuterRing() {
        assertEquals(0, ObservationCenterProgress.radius(20.0D, 0.0D));
        assertEquals(5, ObservationCenterProgress.radius(20.0D, 0.25D));
        assertEquals(10, ObservationCenterProgress.radius(20.0D, 0.50D));
        assertEquals(20, ObservationCenterProgress.radius(20.0D, 1.0D));
    }
}
