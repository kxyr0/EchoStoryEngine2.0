package io.github.kxyr0.echocutscene.core.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraPlaybackClockTest {
    @Test
    void speedHalfSlowsElapsedTime() {
        PlaybackTime time = new CameraPlaybackClock(1000L, 4000L, 0.5D).at(3000L);

        assertEquals(1000L, time.elapsedMillis());
        assertFalse(time.ended());
    }

    @Test
    void speedTwoAcceleratesElapsedTimeAndClamps() {
        PlaybackTime time = new CameraPlaybackClock(1000L, 4000L, 2.0D).at(4000L);

        assertEquals(4000L, time.elapsedMillis());
        assertTrue(time.ended());
    }
}
