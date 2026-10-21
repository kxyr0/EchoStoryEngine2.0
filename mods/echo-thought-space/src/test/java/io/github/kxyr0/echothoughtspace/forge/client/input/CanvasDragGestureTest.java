package io.github.kxyr0.echothoughtspace.forge.client.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import org.junit.jupiter.api.Test;

final class CanvasDragGestureTest {
    @Test
    void separatesClickFromPanAtPhysicalPixelThreshold() {
        CanvasDragGesture gesture = new CanvasDragGesture();
        gesture.begin(new CanvasPoint(100.0D, 100.0D));
        assertTrue(gesture.move(new CanvasPoint(103.0D, 102.0D)).isEmpty());
        assertTrue(gesture.finish());
        gesture.begin(new CanvasPoint(100.0D, 100.0D));
        assertTrue(gesture.move(new CanvasPoint(108.0D, 100.0D)).isPresent());
        assertFalse(gesture.finish());
    }
}
