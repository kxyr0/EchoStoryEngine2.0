package io.github.kxyr0.echoiteminspection.client.input;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kxyr0.echoiteminspection.math.ItemInspectionTransform;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import org.junit.jupiter.api.Test;

class InspectionInputTest {
    @Test void movementWithoutHeldLeftButtonNeverRotates() {
        var transform = new ItemInspectionTransform(ItemInspectionView.defaults());
        var input = new InspectionInput(transform);
        var initial = transform.orientation();
        assertFalse(input.drag(0, 40, 30, 2));
        input.press(1);
        assertFalse(input.drag(1, 40, 30, 2));
        assertEquals(initial, transform.orientation());
    }

    @Test void releaseStopsRotationAndRightButtonCannotContinueLeftDrag() {
        var transform = new ItemInspectionTransform(ItemInspectionView.defaults());
        var input = new InspectionInput(transform);
        input.press(0);
        assertTrue(input.drag(0, 40, 30, 2));
        var rotated = transform.orientation();
        assertFalse(input.drag(1, 80, 60, 2));
        input.release(0);
        assertFalse(input.drag(0, 80, 60, 2));
        assertEquals(rotated, transform.orientation());
    }

    @Test void samePhysicalDragHasSameRotationAtEveryGuiScale() {
        var reference = new ItemInspectionTransform(ItemInspectionView.defaults());
        reference.drag(80, -60);
        for (double scale : new double[] {1, 2, 3, 4}) {
            var transform = new ItemInspectionTransform(ItemInspectionView.defaults());
            var input = new InspectionInput(transform);
            input.press(0);
            input.drag(0, 80 / scale, -60 / scale, scale);
            assertEquals(reference.orientation(), transform.orientation());
        }
    }

    @Test void wheelClampsAndScreenRemovalCancelsDrag() {
        var view = ItemInspectionView.defaults();
        var transform = new ItemInspectionTransform(view);
        var input = new InspectionInput(transform);
        input.scroll(10000);
        assertEquals(view.maxZoom(), transform.zoom());
        input.scroll(-10000);
        assertEquals(view.minZoom(), transform.zoom());
        input.press(0);
        input.cancelDrag();
        assertFalse(input.drag(0, 10, 10, 1));
    }
}
