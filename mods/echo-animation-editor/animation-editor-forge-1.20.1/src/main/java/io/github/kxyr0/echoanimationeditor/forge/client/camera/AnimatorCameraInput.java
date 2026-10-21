package io.github.kxyr0.echoanimationeditor.forge.client.camera;

import io.github.kxyr0.echoanimationeditor.api.camera.AnimatorCameraSettings;
import java.util.Optional;
import org.lwjgl.glfw.GLFW;

public final class AnimatorCameraInput {
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private boolean fast;
    private boolean slow;
    private boolean freeLook;
    private double speedWheel;
    private double lookYaw;
    private double lookPitch;

    public boolean key(int keyCode, boolean pressed) {
        if (keyCode == GLFW.GLFW_KEY_W) {
            forward = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_S) {
            back = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_A) {
            left = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            right = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_E) {
            up = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_Q) {
            down = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            fast = pressed;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) {
            slow = pressed;
        } else {
            return false;
        }
        return true;
    }

    public void freeLook(boolean active) {
        freeLook = active;
    }

    public boolean freeLook() {
        return freeLook;
    }

    public void drag(double dx, double dy) {
        if (freeLook) {
            lookYaw -= dx * 0.12D;
            lookPitch += dy * 0.12D;
        }
    }

    public void wheel(double amount) {
        speedWheel = Math.max(-6.0D, Math.min(8.0D, speedWheel + amount));
    }

    public Optional<LookDelta> consumeLook() {
        if (lookYaw == 0.0D && lookPitch == 0.0D) {
            return Optional.empty();
        }
        LookDelta delta = new LookDelta((float) lookYaw, (float) lookPitch);
        lookYaw = 0.0D;
        lookPitch = 0.0D;
        return Optional.of(delta);
    }

    public double forwardAxis() {
        if (!freeLook) {
            return 0.0D;
        }
        return (forward ? 1.0D : 0.0D) - (back ? 1.0D : 0.0D);
    }

    public double rightAxis() {
        if (!freeLook) {
            return 0.0D;
        }
        return (right ? 1.0D : 0.0D) - (left ? 1.0D : 0.0D);
    }

    public double verticalAxis() {
        if (!freeLook) {
            return 0.0D;
        }
        return (up ? 1.0D : 0.0D) - (down ? 1.0D : 0.0D);
    }

    public double speed(AnimatorCameraSettings settings) {
        double multiplier = Math.pow(1.18D, speedWheel);
        if (fast) {
            multiplier *= settings.fastMultiplier();
        }
        if (slow) {
            multiplier *= settings.slowMultiplier();
        }
        return settings.baseSpeed() * multiplier;
    }

    public record LookDelta(float yawDelta, float pitchDelta) {
    }
}
