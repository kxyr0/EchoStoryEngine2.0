package io.github.kxyr0.echocutscene.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;

public final class ClientCameraMovement {
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double FAST_MULTIPLIER = 3.2D;
    private static final double SLOW_MULTIPLIER = 0.3D;

    private ClientCameraMovement() {
    }

    public static Vector3 delta(
            Minecraft minecraft,
            CameraRotation rotation,
            double frameSeconds,
            double cameraSpeed,
            boolean pitchFlight
    ) {
        Options options = minecraft.options;
        double forward = axis(minecraft, options.keyUp, options.keyDown);
        double side = axis(minecraft, options.keyRight, options.keyLeft);
        double vertical = axis(minecraft, options.keyJump, options.keyShift);
        double speed = speed(minecraft, options, cameraSpeed) * frameSeconds;
        double yaw = Math.toRadians(rotation.yaw());
        double pitch = pitchFlight ? Math.toRadians(rotation.pitch()) : 0.0D;
        double x = forward * -Math.sin(yaw) * Math.cos(pitch) + side * -Math.cos(yaw);
        double y = vertical + forward * -Math.sin(pitch);
        double z = forward * Math.cos(yaw) * Math.cos(pitch) + side * -Math.sin(yaw);
        double length = Math.max(1.0D, Math.sqrt(x * x + y * y + z * z));
        return new Vector3(x / length * speed, y / length * speed, z / length * speed);
    }

    private static double speed(Minecraft minecraft, Options options, double cameraSpeed) {
        double base = cameraSpeed * TICKS_PER_SECOND;
        if (keyDown(minecraft, options.keySprint) || Screen.hasControlDown()) {
            return base * FAST_MULTIPLIER;
        }
        return Screen.hasAltDown() ? base * SLOW_MULTIPLIER : base;
    }

    private static double axis(Minecraft minecraft, KeyMapping positive, KeyMapping negative) {
        return (keyDown(minecraft, positive) ? 1.0D : 0.0D)
                - (keyDown(minecraft, negative) ? 1.0D : 0.0D);
    }

    private static boolean keyDown(Minecraft minecraft, KeyMapping mapping) {
        if (minecraft.getWindow() == null || mapping.getKey().getType() != InputConstants.Type.KEYSYM) {
            return mapping.isDown();
        }
        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), mapping.getKey().getValue());
    }
}
