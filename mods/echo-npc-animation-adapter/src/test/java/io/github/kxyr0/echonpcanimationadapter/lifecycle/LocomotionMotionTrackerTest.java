package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import org.junit.jupiter.api.Test;

final class LocomotionMotionTrackerTest {
    @Test
    void smoothsAbruptSpeedChangesAfterConfirmedRealMovement() {
        var tracker = walking();
        double slow = tracker.speed("jdh:father");
        tracker.observe("jdh:father", 0.45D, 0.0D, 6, NavigationGait.WALK, false);
        double fast = tracker.speed("jdh:father");
        assertTrue(slow > 0.0D);
        assertTrue(fast > slow);
        assertTrue(fast < 1.60D);
        assertTrue(fast - slow <= 0.35D * (1.60D - slow) + 0.000001D);
    }
    @Test
    void navigationIntentWithoutDisplacementCannotStartLocomotion() {
        var tracker = new LocomotionMotionTracker();
        for (long tick = 0; tick < 20; tick++) {
            tracker.observe("jdh:father", 0.0D, 0.0D, tick, NavigationGait.SPRINT, false);
        }
        assertEquals(LocomotionMotionTracker.BodyState.STANDING, tracker.state("jdh:father"));
        assertTrue(tracker.speed("jdh:father") <= 0.1D);
    }
    @Test
    void sittingIsExclusiveAndStandingStartsWithFreshMotionHistory() {
        var tracker = walking();
        assertEquals(LocomotionMotionTracker.BodyState.WALKING, tracker.state("jdh:father"));
        tracker.observe("jdh:father", 0.3D, 0.0D, 6, NavigationGait.WALK, true);
        assertEquals(LocomotionMotionTracker.BodyState.SITTING, tracker.state("jdh:father"));
        tracker.observe("jdh:father", 0.3D, 0.0D, 7, NavigationGait.WALK, false);
        assertEquals(LocomotionMotionTracker.BodyState.STANDING, tracker.state("jdh:father"));
    }
    private LocomotionMotionTracker walking() {
        var tracker = new LocomotionMotionTracker();
        for (long tick = 0; tick <= 5; tick++) {
            tracker.observe("jdh:father", tick * 0.05D, 0.0D, tick, NavigationGait.WALK, false);
        }
        return tracker;
    }
}
