package io.github.kxyr0.echoanimationeditor.core.gizmo;

import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorGizmoHandle;
import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorTransformDelta;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnimatorGizmoPickerTest {
    private static final AnimatorPanelRect VIEWPORT = new AnimatorPanelRect(100, 100, 400, 300);
    private final AnimatorGizmoPicker picker = new AnimatorGizmoPicker();
    private final AnimatorGizmoDeltaMapper mapper = new AnimatorGizmoDeltaMapper();

    @Test
    void translateToolPicksAxesAndPlanes() {
        assertEquals(AnimatorGizmoHandle.TRANSLATE_X, picker.pick(AnimatorTool.TRANSLATE, VIEWPORT, 335, 250));
        assertEquals(AnimatorGizmoHandle.TRANSLATE_Y, picker.pick(AnimatorTool.TRANSLATE, VIEWPORT, 300, 214));
        assertEquals(AnimatorGizmoHandle.TRANSLATE_Z, picker.pick(AnimatorTool.TRANSLATE, VIEWPORT, 274, 276));
        assertEquals(AnimatorGizmoHandle.TRANSLATE_XY, picker.pick(AnimatorTool.TRANSLATE, VIEWPORT, 322, 228));
    }

    @Test
    void rotateToolPicksNestedRings() {
        assertEquals(AnimatorGizmoHandle.ROTATE_Z, picker.pick(AnimatorTool.ROTATE, VIEWPORT, 354, 250));
        assertEquals(AnimatorGizmoHandle.ROTATE_X, picker.pick(AnimatorTool.ROTATE, VIEWPORT, 340, 250));
        assertEquals(AnimatorGizmoHandle.ROTATE_Y, picker.pick(AnimatorTool.ROTATE, VIEWPORT, 326, 250));
    }

    @Test
    void deltaMapperKeepsPlaneAndAxisComponentsSeparate() {
        AnimatorTransformDelta xy = mapper.map(AnimatorGizmoHandle.TRANSLATE_XY, 40.0D, -20.0D);
        AnimatorTransformDelta scaleY = mapper.map(AnimatorGizmoHandle.SCALE_Y, 25.0D, -50.0D);

        assertEquals(1.0D, xy.translateX());
        assertEquals(0.5D, xy.translateY());
        assertEquals(0.0D, xy.translateZ());
        assertEquals(1.5D, scaleY.scaleY());
        assertEquals(1.0D, scaleY.scaleX());
    }
}
