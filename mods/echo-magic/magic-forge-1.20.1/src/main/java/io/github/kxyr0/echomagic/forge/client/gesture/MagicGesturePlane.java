package io.github.kxyr0.echomagic.forge.client.gesture;

import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public record MagicGesturePlane(Vec3 center, Vec3 normal, Vec3 right, Vec3 up) {
    private static final double DISTANCE = 2.25D;
    private static final double THIRD_PERSON_VISUAL_DISTANCE = 1.65D;
    static final double CANVAS_HALF_WIDTH = 0.96D;
    static final double CANVAS_HALF_HEIGHT = 0.72D;

    public static Optional<MagicGesturePlane> facing(LocalPlayer player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 normal = player.getViewVector(partialTick).normalize();
        Vec3 right = normal.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-4D) return Optional.empty();
        right = right.normalize();
        Vec3 up = right.cross(normal).normalize();
        return Optional.of(new MagicGesturePlane(eye.add(normal.scale(DISTANCE)), normal, right, up));
    }

    public Optional<Projection> project(LocalPlayer player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 direction = player.getViewVector(partialTick).normalize();
        return project(eye, direction).map(value -> new Projection(value.x(), value.y(),
                visibleWorld(value.world())));
    }

    Projection projectLocal(LocalPlayer player, float partialTick, double x, double y) {
        double localX = Math.max(-CANVAS_HALF_WIDTH, Math.min(CANVAS_HALF_WIDTH, x));
        double localY = Math.max(-CANVAS_HALF_HEIGHT, Math.min(CANVAS_HALF_HEIGHT, y));
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 movingCenter = eye.add(normal.scale(DISTANCE));
        Vec3 world = movingCenter.add(right.scale(localX)).add(up.scale(localY));
        return new Projection(localX, localY, visibleWorld(world));
    }

    Optional<Projection> project(Vec3 eye, Vec3 direction) {
        direction = direction.normalize();
        double denominator = direction.dot(normal);
        if (denominator < 0.2D) return Optional.empty();
        Vec3 movingCenter = eye.add(normal.scale(DISTANCE));
        double distance = movingCenter.subtract(eye).dot(normal) / denominator;
        if (distance < 0.25D || distance > 8.0D) return Optional.empty();
        Vec3 captureWorld = eye.add(direction.scale(distance));
        Vec3 relative = captureWorld.subtract(movingCenter);
        double x = Math.max(-CANVAS_HALF_WIDTH, Math.min(CANVAS_HALF_WIDTH, relative.dot(right)));
        double y = Math.max(-CANVAS_HALF_HEIGHT, Math.min(CANVAS_HALF_HEIGHT, relative.dot(up)));
        return Optional.of(new Projection(x, y, captureWorld));
    }

    private static Vec3 visibleWorld(Vec3 firstPersonPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType().isFirstPerson()) return firstPersonPosition;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return firstPersonPosition;
        Vector3f look = camera.getLookVector();
        Vec3 direction = new Vec3(look.x(), look.y(), look.z()).normalize();
        return camera.getPosition().add(direction.scale(THIRD_PERSON_VISUAL_DISTANCE));
    }

    public record Projection(double x, double y, Vec3 world) { }
}
