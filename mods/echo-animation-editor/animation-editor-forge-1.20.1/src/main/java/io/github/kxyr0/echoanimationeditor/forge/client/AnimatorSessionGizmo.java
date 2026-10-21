package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorGizmoHandle;
import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorTransformDelta;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.core.gizmo.AnimatorGizmoDeltaMapper;
import io.github.kxyr0.echoanimationeditor.core.gizmo.AnimatorGizmoPicker;
import java.util.Optional;

final class AnimatorSessionGizmo {
    private final AnimatorGizmoPicker picker = new AnimatorGizmoPicker();
    private final AnimatorGizmoDeltaMapper mapper = new AnimatorGizmoDeltaMapper();
    private AnimatorGizmoHandle activeHandle = AnimatorGizmoHandle.NONE;
    private double startX;
    private double startY;

    boolean begin(AnimatorTool tool, AnimatorPanelRect viewport, double mouseX, double mouseY) {
        activeHandle = picker.pick(tool, viewport, mouseX, mouseY);
        if (activeHandle == AnimatorGizmoHandle.NONE) {
            return false;
        }
        startX = mouseX;
        startY = mouseY;
        return true;
    }

    Optional<AnimatorTransformDelta> finish(double mouseX, double mouseY) {
        AnimatorGizmoHandle handle = activeHandle;
        activeHandle = AnimatorGizmoHandle.NONE;
        if (handle == AnimatorGizmoHandle.NONE) {
            return Optional.empty();
        }
        return Optional.of(mapper.map(handle, mouseX - startX, mouseY - startY));
    }

    AnimatorGizmoHandle activeHandle() {
        return activeHandle;
    }
}
