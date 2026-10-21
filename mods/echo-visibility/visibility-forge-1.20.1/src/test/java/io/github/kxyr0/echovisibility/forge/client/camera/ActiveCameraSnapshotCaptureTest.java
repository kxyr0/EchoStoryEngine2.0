package io.github.kxyr0.echovisibility.forge.client.camera;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ActiveCameraSnapshotCaptureTest {
    @Test
    void acceptsFiniteProjection() {
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(matrix(1.0F)).isPresent());
    }

    @Test
    void skipsProjectionWithNonFiniteValue() {
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(matrix(Float.NaN)).isEmpty());
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(matrix(Float.POSITIVE_INFINITY)).isEmpty());
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(matrix(Float.NEGATIVE_INFINITY)).isEmpty());
    }

    @Test
    void skipsMalformedProjection() {
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(null).isEmpty());
        assertTrue(ActiveCameraSnapshotCapture.finiteProjection(new float[15]).isEmpty());
    }

    private static float[] matrix(float selectedValue) {
        float[] values = new float[16];
        Arrays.fill(values, 1.0F);
        values[7] = selectedValue;
        return values;
    }
}
