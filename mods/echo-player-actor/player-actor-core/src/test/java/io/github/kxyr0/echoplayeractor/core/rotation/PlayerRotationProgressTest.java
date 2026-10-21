package io.github.kxyr0.echoplayeractor.core.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import org.junit.jupiter.api.Test;

class PlayerRotationProgressTest {
    private final PlayerRotationProgress progress = new PlayerRotationProgress();

    @Test
    void usesShortestYawPathAcrossZero() {
        PlayerActorRotationSample sample =
                progress.sample(359.0D, 0.0D, 1.0D, 0.0D, 10, 5, PlayerActorInterpolation.LINEAR);

        assertEquals(0.0D, sample.yaw(), 0.001D);
    }

    @Test
    void clampsPitchAndCompletesImmediateRotation() {
        PlayerActorRotationSample sample =
                progress.sample(0.0D, 0.0D, 90.0D, 120.0D, 0, 0, PlayerActorInterpolation.SMOOTH);

        assertTrue(sample.complete());
        assertEquals(90.0D, sample.pitch(), 0.001D);
    }
}
