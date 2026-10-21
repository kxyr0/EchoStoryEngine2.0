package io.github.kxyr0.echoanimationeditor.core.gizmo;

import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorGizmoHandle;
import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorTransformDelta;

public final class AnimatorGizmoDeltaMapper {
    public AnimatorTransformDelta map(AnimatorGizmoHandle handle, double dx, double dy) {
        if (handle.name().startsWith("TRANSLATE")) {
            return translateHandle(handle, dx, dy);
        }
        if (handle.name().startsWith("ROTATE")) {
            return rotateHandle(handle, dx, dy);
        }
        if (handle.name().startsWith("SCALE")) {
            return scaleHandle(handle, dx, dy);
        }
        return AnimatorTransformDelta.none();
    }

    private AnimatorTransformDelta translateHandle(AnimatorGizmoHandle handle, double dx, double dy) {
        double moveX = dx / 40.0D;
        double moveY = -dy / 40.0D;
        double moveZ = (dx - dy) / 80.0D;
        return switch (handle) {
            case TRANSLATE_X -> translate(moveX, 0.0D, 0.0D);
            case TRANSLATE_Y -> translate(0.0D, moveY, 0.0D);
            case TRANSLATE_Z -> translate(0.0D, 0.0D, moveZ);
            case TRANSLATE_XY -> translate(moveX, moveY, 0.0D);
            case TRANSLATE_XZ -> translate(moveX, 0.0D, moveZ);
            case TRANSLATE_YZ -> translate(0.0D, moveY, moveZ);
            default -> AnimatorTransformDelta.none();
        };
    }

    private AnimatorTransformDelta rotateHandle(AnimatorGizmoHandle handle, double dx, double dy) {
        return switch (handle) {
            case ROTATE_X -> rotate(dx, 0.0D, 0.0D);
            case ROTATE_Y -> rotate(0.0D, -dy, 0.0D);
            case ROTATE_Z -> rotate(0.0D, 0.0D, dx - dy);
            default -> AnimatorTransformDelta.none();
        };
    }

    private AnimatorTransformDelta scaleHandle(AnimatorGizmoHandle handle, double dx, double dy) {
        double scaleX = Math.max(0.05D, 1.0D + dx / 100.0D);
        double scaleY = Math.max(0.05D, 1.0D - dy / 100.0D);
        double scaleZ = Math.max(0.05D, 1.0D + (dx - dy) / 160.0D);
        return switch (handle) {
            case SCALE_X -> scale(scaleX, 1.0D, 1.0D);
            case SCALE_Y -> scale(1.0D, scaleY, 1.0D);
            case SCALE_Z -> scale(1.0D, 1.0D, scaleZ);
            case SCALE_UNIFORM -> scale(scaleZ, scaleZ, scaleZ);
            default -> AnimatorTransformDelta.none();
        };
    }

    private static AnimatorTransformDelta translate(double x, double y, double z) {
        return new AnimatorTransformDelta(x, y, z, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    private static AnimatorTransformDelta rotate(double x, double y, double z) {
        return new AnimatorTransformDelta(0.0D, 0.0D, 0.0D, x, y, z, 1.0D, 1.0D, 1.0D);
    }

    private static AnimatorTransformDelta scale(double x, double y, double z) {
        return new AnimatorTransformDelta(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, x, y, z);
    }
}
