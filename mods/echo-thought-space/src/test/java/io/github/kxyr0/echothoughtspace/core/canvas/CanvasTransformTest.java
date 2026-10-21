package io.github.kxyr0.echothoughtspace.core.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class CanvasTransformTest {
    @Test
    void logicalPositionIgnoresGuiScale() {
        CanvasCamera camera = new CanvasCamera();
        CanvasPoint point = new CanvasPoint(300.0D, -120.0D);
        CanvasTransform guiTwo = CanvasTransform.fit(camera, new CanvasViewport(1920, 1080, 960, 540));
        CanvasTransform guiFour = CanvasTransform.fit(camera, new CanvasViewport(1920, 1080, 480, 270));
        assertEquals(guiTwo.toPhysical(point), guiFour.toPhysical(point));
    }

    @Test
    void inverseTransformSurvivesArbitraryAspectRatioAndCameraPan() {
        CanvasCamera camera = new CanvasCamera();
        CanvasTransform initial = CanvasTransform.fit(camera, new CanvasViewport(3440, 1440, 1720, 720));
        camera.pan(new CanvasVector(170.0D, -90.0D), initial);
        CanvasTransform moved = CanvasTransform.fit(camera, initial.viewport());
        CanvasPoint expected = new CanvasPoint(-712.5D, 91.25D);
        CanvasPoint actual = moved.toCanvas(moved.toPhysical(expected));
        assertEquals(expected.x(), actual.x(), 0.000001D);
        assertEquals(expected.y(), actual.y(), 0.000001D);
    }

    @Test
    void zoomKeepsCanvasPointUnderCursor() {
        CanvasCamera camera = new CanvasCamera();
        CanvasViewport viewport = new CanvasViewport(1920, 1080, 960, 540);
        CanvasPoint cursor = new CanvasPoint(1420.0D, 310.0D);
        CanvasPoint before = CanvasTransform.fit(camera, viewport).toCanvas(cursor);
        camera.zoomAt(cursor, 2.25D, viewport);
        CanvasPoint after = CanvasTransform.fit(camera, viewport).toCanvas(cursor);
        assertEquals(before.x(), after.x(), 0.000001D);
        assertEquals(before.y(), after.y(), 0.000001D);
        assertEquals(2.25D, camera.zoom(), 0.000001D);
    }

    @Test
    void panUsesTotalZoomScale() {
        CanvasCamera camera = new CanvasCamera();
        CanvasViewport viewport = new CanvasViewport(1920, 1080, 960, 540);
        camera.zoomAt(new CanvasPoint(960.0D, 540.0D), 2.0D, viewport);
        camera.pan(new CanvasVector(200.0D, 80.0D), CanvasTransform.fit(camera, viewport));
        assertEquals(-100.0D, camera.center().x(), 0.000001D);
        assertEquals(-40.0D, camera.center().y(), 0.000001D);
    }
}
