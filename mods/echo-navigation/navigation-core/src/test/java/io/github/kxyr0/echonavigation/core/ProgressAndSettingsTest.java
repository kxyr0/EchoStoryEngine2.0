package io.github.kxyr0.echonavigation.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.core.progress.NavigationStuckDetector;
import io.github.kxyr0.echonavigation.core.progress.NavigationProgressTracker;
import io.github.kxyr0.echonavigation.core.settings.NavigationSettingsValidator;
import org.junit.jupiter.api.Test;

final class ProgressAndSettingsTest {
    @Test
    void defaultsAreAccepted() {
        assertTrue(new NavigationSettingsValidator().validate(NavigationSettingsSnapshot.defaults()).isEmpty());
    }

    @Test
    void stuckRequiresMovingStatusAndTimeout() {
        NavigationSettingsSnapshot settings = NavigationSettingsSnapshot.defaults();
        NavigationProgressSnapshot progress = NavigationProgressSnapshot.initial(new Vector3(0.0D, 64.0D, 0.0D),
                10.0D, 1L);
        NavigationStuckDetector detector = new NavigationStuckDetector();
        assertFalse(detector.stuck(NavigationStatus.WAITING_FOR_MATERIALIZATION, progress, 1000L, settings));
        assertTrue(detector.stuck(NavigationStatus.MOVING, progress, 101L, settings));
    }

    @Test
    void waypointAdvanceCountsAsProgress() {
        NavigationSettingsSnapshot settings = NavigationSettingsSnapshot.defaults();
        NavigationProgressSnapshot previous = NavigationProgressSnapshot.initial(
                new Vector3(0.0D, 64.0D, 0.0D), 10.0D, 1L);
        var sample = new NavigationProgressTracker().sample(previous,
                new Vector3(0.5D, 64.0D, 0.5D), 10.0D, 1, 20L, settings);
        assertEquals(20L, sample.progress().lastProgressTick());
    }
}
