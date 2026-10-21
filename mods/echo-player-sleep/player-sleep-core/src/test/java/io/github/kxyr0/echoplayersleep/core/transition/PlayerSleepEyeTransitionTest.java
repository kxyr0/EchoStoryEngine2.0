package io.github.kxyr0.echoplayersleep.core.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import org.junit.jupiter.api.Test;

final class PlayerSleepEyeTransitionTest {
    @Test
    void coverageDependsOnElapsedTimeNotFrameCount() {
        PlayerSleepEyeTransition transition = new PlayerSleepEyeTransition(
                PlayerSleepRevision.initial(), 100L, Ticks.of(20L), true, false);

        double atHalf = transition.snapshotAt(110L, false).coverage();
        double atEnd = transition.snapshotAt(120L, true).coverage();

        assertEquals(0.5D, atHalf, 0.0001D);
        assertEquals(1.0D, atEnd, 0.0001D);
    }

    @Test
    void closedFrameBarrierRequiresMatchingAckAndBlackFrame() {
        PlayerSleepClosedFrameBarrier barrier = new PlayerSleepClosedFrameBarrier(PlayerSleepRevision.initial());

        assertTrue(barrier.accept(new ClientTransitionAck(PlayerSleepRevision.initial(), true, 1.0D)));
    }
}
