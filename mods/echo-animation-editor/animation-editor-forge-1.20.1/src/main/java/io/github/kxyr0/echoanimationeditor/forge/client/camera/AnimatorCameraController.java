package io.github.kxyr0.echoanimationeditor.forge.client.camera;

import io.github.kxyr0.echoanimationeditor.api.camera.AnimatorCameraPose;
import io.github.kxyr0.echoanimationeditor.api.camera.AnimatorCameraSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;

public final class AnimatorCameraController {
    private final Minecraft minecraft;
    private final Marker camera;
    private AnimatorCameraPose pose;
    private AnimatorCameraSettings settings = AnimatorCameraSettings.defaults();

    private AnimatorCameraController(Minecraft minecraft, Marker camera, AnimatorCameraPose pose) {
        this.minecraft = minecraft;
        this.camera = camera;
        this.pose = pose;
    }

    public static AnimatorCameraController detach(Minecraft minecraft) {
        Marker marker = new Marker(EntityType.MARKER, minecraft.level);
        AnimatorCameraPose pose = new AnimatorCameraPose(
                minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ(),
                minecraft.player.getYRot(), minecraft.player.getXRot());
        apply(marker, pose);
        minecraft.setCameraEntity(marker);
        return new AnimatorCameraController(minecraft, marker, pose);
    }

    public AnimatorCameraPose pose() {
        return pose;
    }

    public void tick(AnimatorCameraInput input) {
        if (!attached()) {
            return;
        }
        input.consumeLook().ifPresent(delta -> pose = pose.rotate(delta.yawDelta(), delta.pitchDelta()));
        double speed = input.speed(settings);
        double yaw = Math.toRadians(pose.yaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double dx = (input.forwardAxis() * forwardX + input.rightAxis() * rightX) * speed;
        double dz = (input.forwardAxis() * forwardZ + input.rightAxis() * rightZ) * speed;
        double dy = input.verticalAxis() * speed;
        pose = pose.move(dx, dy, dz);
        apply(camera, pose);
    }

    public void restore(AnimatorCameraStateSnapshot snapshot) {
        snapshot.restore(minecraft);
    }

    private boolean attached() {
        return minecraft.getCameraEntity() == camera;
    }

    private static void apply(Marker marker, AnimatorCameraPose pose) {
        marker.moveTo(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
        marker.yRotO = pose.yaw();
        marker.xRotO = pose.pitch();
    }
}
