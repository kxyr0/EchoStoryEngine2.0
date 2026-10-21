package io.github.kxyr0.echothoughtspace.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ThoughtSpaceTransitionTest {
    @Test
    void captureCoversOpeningAndEveryClosingFrame() {
        ThoughtSpaceTransition value = new ThoughtSpaceTransition(250L);
        value.open(0L);
        assertEquals(ThoughtSpaceState.OPENING, value.state());
        assertTrue(value.captureActive());
        value.update(250_000_000L);
        assertEquals(ThoughtSpaceState.OPEN, value.state());
        value.close(300_000_000L);
        assertTrue(value.captureActive());
        assertTrue(value.closeFrameComplete(550_000_000L));
        assertTrue(value.captureActive());
        value.finishClosed();
        assertFalse(value.captureActive());
    }

    @Test
    void fadeIsSmoothAndReachesTrueEndpoints() {
        ThoughtSpaceTransition value = new ThoughtSpaceTransition(250L);
        value.open(0L);
        assertEquals(0.0D, value.opacity(0L));
        double middle = value.opacity(125_000_000L);
        assertTrue(middle > 0.0D && middle < 1.0D);
        value.update(250_000_000L);
        assertEquals(1.0D, value.opacity(250_000_000L));
    }
}

