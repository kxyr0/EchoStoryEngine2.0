package io.github.kxyr0.echovisibility.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echovisibility.api.policy.VisibilityHysteresis;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import org.junit.jupiter.api.Test;

class VisibilityTransitionStateTest {
    @Test
    void requiresVisibilityBeforeLostSight() {
        VisibilityTransitionState state = new VisibilityTransitionState(new VisibilityHysteresis(1, 2), true);
        assertEquals(VisibilityTransitionKind.NONE,
                state.observe(VisibilityState.OUTSIDE_FRUSTUM, 1L).transition().kind());
        assertEquals(VisibilityWatchStatus.ARMED_WAITING_FOR_VISIBLE,
                state.observe(VisibilityState.OUTSIDE_FRUSTUM, 2L).status());
        assertFalse(state.everVisible());
    }

    @Test
    void rapidTurnLosesOnFirstFullyHiddenFrame() {
        VisibilityTransitionState state = new VisibilityTransitionState(new VisibilityHysteresis(1, 2), true);
        state.observe(VisibilityState.VISIBLE, 1L);
        VisibilityTransitionResult firstVisible = state.observe(VisibilityState.VISIBLE, 2L);
        assertEquals(VisibilityTransitionKind.FIRST_VISIBLE, firstVisible.transition().kind());
        VisibilityTransitionResult lost = state.observe(VisibilityState.OUTSIDE_FRUSTUM, 3L);
        assertEquals(VisibilityTransitionKind.LOST_SIGHT, lost.transition().kind());
        assertEquals(VisibilityWatchStatus.LOST_SIGHT, lost.status());
        assertTrue(lost.reportable());
    }

    @Test
    void partialVisibilityNeverPublishesLostSight() {
        VisibilityTransitionState state = new VisibilityTransitionState(new VisibilityHysteresis(1, 2), true);
        state.observe(VisibilityState.VISIBLE, 1L);
        state.observe(VisibilityState.VISIBLE, 2L);
        VisibilityTransitionResult partial = state.observe(VisibilityState.PARTIALLY_VISIBLE, 3L);
        assertEquals(VisibilityTransitionKind.NONE, partial.transition().kind());
        assertEquals(VisibilityWatchStatus.TRACKING_VISIBLE, partial.status());
    }

    @Test
    void regainedSightRequiresSeenHysteresis() {
        VisibilityTransitionState state = new VisibilityTransitionState(new VisibilityHysteresis(1, 2), true);
        state.observe(VisibilityState.VISIBLE, 1L);
        state.observe(VisibilityState.VISIBLE, 2L);
        state.observe(VisibilityState.OUTSIDE_FRUSTUM, 3L);
        assertEquals(VisibilityTransitionKind.NONE,
                state.observe(VisibilityState.VISIBLE, 4L).transition().kind());
        assertEquals(VisibilityTransitionKind.GAINED_SIGHT,
                state.observe(VisibilityState.VISIBLE, 5L).transition().kind());
    }

    @Test
    void unchangedStableStateDoesNotRequestReport() {
        VisibilityTransitionState state = new VisibilityTransitionState(new VisibilityHysteresis(1, 1), true);
        assertTrue(state.observe(VisibilityState.VISIBLE, 1L).reportable());
        assertFalse(state.observe(VisibilityState.VISIBLE, 2L).reportable());
        assertFalse(state.observe(VisibilityState.VISIBLE, 3L).reportable());
    }
}
