package io.github.kxyr0.echoitemattachments.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HandVelocityWindowTest {
    @Test
    void samplesServerTicksAndClampsVelocity() {
        HandVelocityWindow window = new HandVelocityWindow();
        window.sample(10L, new Vector3d(0.0D, 0.0D, 0.0D));
        window.sample(12L, new Vector3d(8.0D, 0.0D, 0.0D));
        assertEquals(1.25D, window.velocity(1.25D).orElseThrow().length(), 0.00001D);
    }
}
