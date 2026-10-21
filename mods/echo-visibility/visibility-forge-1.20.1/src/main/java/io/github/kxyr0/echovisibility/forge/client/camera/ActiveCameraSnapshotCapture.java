package io.github.kxyr0.echovisibility.forge.client.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ActiveCameraSnapshotCapture {
    public Optional<VisibilityCameraSnapshot> capture(Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getWindow().getWidth() < 1
                || minecraft.getWindow().getHeight() < 1) {
            return Optional.empty();
        }
        float[] projectionValues = new float[16];
        RenderSystem.getProjectionMatrix().get(projectionValues);
        Optional<VisibilityProjectionMatrix> projection = finiteProjection(projectionValues);
        if (projection.isEmpty()) {
            return Optional.empty();
        }
        Vec3 playerEyes = minecraft.player.getEyePosition(partialTick);
        Vec3 playerForward = minecraft.player.getViewVector(partialTick);
        return Optional.of(new VisibilityCameraSnapshot(source(camera, minecraft),
                VisibilityViewpoint.ACTIVE_CAMERA, vector(camera.getPosition()), vector(camera.getLookVector()),
                vector(camera.getUpVector()), vector(camera.getLeftVector()).scale(-1.0D),
                camera.getYRot(), camera.getXRot(), projection.orElseThrow(),
                minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), revision(projectionValues),
                vector(playerEyes), vector(playerForward), minecraft.player.getYRot(), minecraft.player.getXRot()));
    }

    static Optional<VisibilityProjectionMatrix> finiteProjection(float[] values) {
        if (values == null || values.length != 16) {
            return Optional.empty();
        }
        double[] projection = new double[values.length];
        for (int index = 0; index < values.length; index++) {
            if (!Float.isFinite(values[index])) {
                return Optional.empty();
            }
            projection[index] = values[index];
        }
        return Optional.of(new VisibilityProjectionMatrix(projection));
    }

    public VisibilityCameraSnapshot viewpoint(
            VisibilityCameraSnapshot camera,
            VisibilityViewpoint viewpoint,
            float partialTick) {
        if (viewpoint == VisibilityViewpoint.ACTIVE_CAMERA) {
            return camera;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 up = minecraft.player.getUpVector(partialTick);
        VisibilityVector3 playerUp = vector(up).normalized();
        VisibilityVector3 right = camera.playerForward().cross(playerUp).normalized();
        return new VisibilityCameraSnapshot("PLAYER_EYES", viewpoint, camera.playerEyePosition(),
                camera.playerForward(), playerUp, right, camera.playerYaw(), camera.playerPitch(),
                camera.projection(), camera.framebufferWidth(), camera.framebufferHeight(),
                camera.projectionRevision(), camera.playerEyePosition(), camera.playerForward(),
                camera.playerYaw(), camera.playerPitch());
    }

    private static String source(Camera camera, Minecraft minecraft) {
        String entity = camera.getEntity() == null ? "none"
                : BuiltInRegistries.ENTITY_TYPE.getKey(camera.getEntity().getType()).toString();
        String kind = minecraft.getCameraEntity() == minecraft.player ? "PLAYER" : "DETACHED";
        return kind + ":" + minecraft.options.getCameraType().name() + ":" + entity;
    }

    private static long revision(float[] projection) {
        long result = 1L;
        for (float value : projection) {
            result = 31L * result + Float.floatToIntBits(value);
        }
        return result & Long.MAX_VALUE;
    }

    private static VisibilityVector3 vector(Vec3 value) {
        return new VisibilityVector3(value.x, value.y, value.z);
    }

    private static VisibilityVector3 vector(Vector3f value) {
        return new VisibilityVector3(value.x(), value.y(), value.z());
    }
}
