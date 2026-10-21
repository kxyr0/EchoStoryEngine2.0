package io.github.kxyr0.echoiteminspection.math;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemInspectionTransformTest {
    @Test
    void modelLocalAnchorActuallyRotatesInThreeDimensions() {
        var point = QuaternionRotation.axisY(90).rotate(1, 0, 0);
        assertEquals(0, point.x(), 1.0e-12);
        assertEquals(0, point.y(), 1.0e-12);
        assertEquals(-1, point.z(), 1.0e-12);
    }

    @Test
    void dragCrossesNinetyDegreesWithoutLosingRotationAxisOrQuaternionNormalization() {
        var transform = new ItemInspectionTransform(zeroView());
        transform.drag(0, 89);
        var before = transform.orientation().rotate(0, 1, 0);
        transform.drag(0, 2);
        var after = transform.orientation().rotate(0, 1, 0);
        assertTrue(before.y() > 0);
        assertTrue(after.y() < 0);
        assertEquals(before.z(), after.z(), 1.0e-12);
        assertEquals(1, norm(transform.orientation()), 1.0e-12);
    }

    @Test
    void longCompoundDragRemainsNormalizedAndInverseDeltasRestoreOrientation() {
        var transform = new ItemInspectionTransform(zeroView());
        for (int index = 0; index < 20000; index++) {
            transform.drag(2.3, 0);
            transform.drag(0, -1.7);
            transform.drag(0, 1.7);
            transform.drag(-2.3, 0);
        }
        assertEquals(1, norm(transform.orientation()), 1.0e-12);
        var point = transform.orientation().rotate(1, 0, 0);
        assertEquals(1, point.x(), 1.0e-10);
        assertEquals(0, point.y(), 1.0e-10);
        assertEquals(0, point.z(), 1.0e-10);
    }

    @Test
    void zoomIsMonotonicClampedAndExtremeWheelValuesDoNotOverflowState() {
        var transform = new ItemInspectionTransform(ItemInspectionView.defaults());
        transform.scroll(1);
        assertTrue(transform.zoom() > 1);
        transform.scroll(Double.MAX_VALUE);
        assertEquals(transform.view().maxZoom(), transform.zoom());
        transform.scroll(-Double.MAX_VALUE);
        assertEquals(transform.view().minZoom(), transform.zoom());
        transform.scroll(Double.NaN);
        assertEquals(transform.view().minZoom(), transform.zoom());
    }

    @Test
    void resetRestoresAuthoredOrientationAndZoomAndInvalidInputIsIgnored() {
        var transform = new ItemInspectionTransform(ItemInspectionView.defaults());
        var original = transform.orientation();
        transform.drag(65, 23);
        transform.scroll(4);
        assertNotEquals(original, transform.orientation());
        transform.reset();
        assertEquals(original, transform.orientation());
        assertEquals(1, transform.zoom());
        assertEquals(12, transform.rotationX(), 1.0e-12);
        assertEquals(-20, transform.rotationY(), 1.0e-12);
        assertEquals(0, transform.rotationZ(), 1.0e-12);
        transform.drag(Double.NaN, 0);
        assertEquals(original, transform.orientation());
    }

    @Test
    void invalidProgrammaticPresentationCannotEnterSession() {
        assertThrows(IllegalArgumentException.class,
                () -> new ItemInspectionView(1, Double.NaN, 0, 0, 1, 2, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemInspectionView(Double.POSITIVE_INFINITY, 0, 0, 0, 1, 2, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new QuaternionRotation(0, 0, 0, 0));
    }

    @Test
    void finiteExtremeSensitivityAndDragCannotOverflowQuaternionState() {
        for (double sensitivity : new double[]{Double.MAX_VALUE, Double.MIN_VALUE, 0.35}) {
            var view = new ItemInspectionView(1, 0, 0, 0, 0.5, 3, 1, sensitivity, 0.12);
            var transform = new ItemInspectionTransform(view);
            assertDoesNotThrow(() -> transform.drag(Double.MAX_VALUE, -Double.MAX_VALUE));
            assertEquals(1, norm(transform.orientation()), 1.0e-12);
            assertTrue(Double.isFinite(transform.rotationX()));
            assertTrue(Double.isFinite(transform.rotationY()));
            assertTrue(Double.isFinite(transform.rotationZ()));
        }
    }

    private ItemInspectionView zeroView() {
        return new ItemInspectionView(1, 0, 0, 0, 0.5, 3, 1, 1, 0.12);
    }

    private double norm(QuaternionRotation value) {
        return value.x() * value.x() + value.y() * value.y() + value.z() * value.z() + value.w() * value.w();
    }
}
