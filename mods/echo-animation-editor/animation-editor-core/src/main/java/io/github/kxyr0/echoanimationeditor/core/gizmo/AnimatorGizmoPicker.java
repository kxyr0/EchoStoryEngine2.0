package io.github.kxyr0.echoanimationeditor.core.gizmo;

import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorGizmoHandle;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;

public final class AnimatorGizmoPicker {
    private static final double AXIS_LENGTH = 58.0D;
    private static final double AXIS_TOLERANCE = 7.0D;
    private static final double PLANE_MIN = 12.0D;
    private static final double PLANE_MAX = 34.0D;
    private static final double CENTER = 10.0D;

    public AnimatorGizmoHandle pick(AnimatorTool tool, AnimatorPanelRect viewport, double mouseX, double mouseY) {
        double cx = viewport.left() + viewport.width() / 2.0D;
        double cy = viewport.top() + viewport.height() / 2.0D;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        return switch (tool) {
            case SELECT -> pickSelect(dx, dy);
            case TRANSLATE -> pickTranslate(dx, dy);
            case ROTATE -> pickRotate(dx, dy);
            case SCALE -> pickScale(dx, dy);
        };
    }

    private AnimatorGizmoHandle pickSelect(double dx, double dy) {
        return distance(dx, dy) <= CENTER ? AnimatorGizmoHandle.SCALE_UNIFORM : AnimatorGizmoHandle.NONE;
    }

    private AnimatorGizmoHandle pickTranslate(double dx, double dy) {
        AnimatorGizmoHandle plane = plane(dx, dy);
        if (plane != AnimatorGizmoHandle.NONE) {
            return plane;
        }
        if (axis(dx, dy)) {
            return AnimatorGizmoHandle.TRANSLATE_X;
        }
        if (axis(-dy, dx)) {
            return AnimatorGizmoHandle.TRANSLATE_Y;
        }
        return diagonal(dx, dy) ? AnimatorGizmoHandle.TRANSLATE_Z : AnimatorGizmoHandle.NONE;
    }

    private AnimatorGizmoHandle pickScale(double dx, double dy) {
        if (distance(dx, dy) <= CENTER) {
            return AnimatorGizmoHandle.SCALE_UNIFORM;
        }
        if (axis(dx, dy)) {
            return AnimatorGizmoHandle.SCALE_X;
        }
        if (axis(-dy, dx)) {
            return AnimatorGizmoHandle.SCALE_Y;
        }
        return diagonal(dx, dy) ? AnimatorGizmoHandle.SCALE_Z : AnimatorGizmoHandle.NONE;
    }

    private AnimatorGizmoHandle pickRotate(double dx, double dy) {
        double radius = distance(dx, dy);
        if (near(radius, 54.0D)) {
            return AnimatorGizmoHandle.ROTATE_Z;
        }
        if (near(radius, 40.0D)) {
            return AnimatorGizmoHandle.ROTATE_X;
        }
        return near(radius, 26.0D) ? AnimatorGizmoHandle.ROTATE_Y : AnimatorGizmoHandle.NONE;
    }

    private AnimatorGizmoHandle plane(double dx, double dy) {
        if (between(dx) && between(-dy)) {
            return AnimatorGizmoHandle.TRANSLATE_XY;
        }
        if (between(dx) && between(dy)) {
            return AnimatorGizmoHandle.TRANSLATE_XZ;
        }
        return between(-dx) && between(-dy) ? AnimatorGizmoHandle.TRANSLATE_YZ : AnimatorGizmoHandle.NONE;
    }

    private static boolean axis(double primary, double secondary) {
        return primary >= CENTER && primary <= AXIS_LENGTH && Math.abs(secondary) <= AXIS_TOLERANCE;
    }

    private static boolean diagonal(double dx, double dy) {
        return dx <= -CENTER && dy >= CENTER && Math.abs(Math.abs(dx) - Math.abs(dy)) <= AXIS_TOLERANCE
                && distance(dx, dy) <= AXIS_LENGTH;
    }

    private static boolean between(double value) {
        return value >= PLANE_MIN && value <= PLANE_MAX;
    }

    private static boolean near(double value, double target) {
        return Math.abs(value - target) <= AXIS_TOLERANCE;
    }

    private static double distance(double dx, double dy) {
        return Math.sqrt(dx * dx + dy * dy);
    }
}
