package io.github.kxyr0.echothoughtspace.forge.client.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import org.junit.jupiter.api.Test;

final class ThoughtConnectionGestureTest {
    @Test
    void rejectedConnectionRetractsAndDisappears() {
        ThoughtConnectionGesture gesture = new ThoughtConnectionGesture();
        CanvasPoint source = new CanvasPoint(0.0D, 0.0D);
        gesture.begin(new ThoughtNodeId("a"), source, new CanvasPoint(100.0D, 100.0D));
        gesture.move(new CanvasPoint(120.0D, 100.0D), new CanvasPoint(80.0D, 0.0D), false);
        assertTrue(gesture.visual(1L).isPresent());
        long releasedAt = 1_000_000_000L;
        gesture.release(false, releasedAt);
        var halfway = gesture.visual(releasedAt + 140_000_000L).orElseThrow();
        assertTrue(halfway.endpoint().x() < 80.0D);
        assertTrue(halfway.opacity() < 1.0D);
        assertTrue(gesture.visual(releasedAt + 300_000_000L).isEmpty());
    }

    @Test
    void successfulConnectionClearsImmediately() {
        ThoughtConnectionGesture gesture = new ThoughtConnectionGesture();
        gesture.begin(new ThoughtNodeId("a"), new CanvasPoint(0.0D, 0.0D),
                new CanvasPoint(100.0D, 100.0D));
        gesture.move(new CanvasPoint(120.0D, 100.0D), new CanvasPoint(80.0D, 0.0D), true);
        assertTrue(gesture.dragged());
        gesture.release(true, 1L);
        assertFalse(gesture.active());
        assertEquals(java.util.Optional.empty(), gesture.visual(2L));
    }
}
