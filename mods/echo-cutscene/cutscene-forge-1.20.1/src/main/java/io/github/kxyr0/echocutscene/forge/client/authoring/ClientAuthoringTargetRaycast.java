package io.github.kxyr0.echocutscene.forge.client.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientAuthoringTargetRaycast {
    private static final double MAX_DISTANCE = 64.0D;

    public Optional<CameraTarget> fixedTarget(CameraPose pose) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || pose == null) {
            return Optional.empty();
        }
        Vec3 from = origin(pose);
        Vec3 to = from.add(direction(pose.rotation()).scale(MAX_DISTANCE));
        ClipContext context = new ClipContext(from, to, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, minecraft.player);
        BlockHitResult hit = minecraft.level.clip(context);
        Vec3 selected = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        return Optional.of(CameraTarget.fixed(new Vector3(selected.x, selected.y, selected.z)));
    }

    private static Vec3 origin(CameraPose pose) {
        Vector3 position = pose.position().value();
        return new Vec3(position.x(), position.y(), position.z());
    }

    private static Vec3 direction(CameraRotation rotation) {
        double yaw = Math.toRadians(rotation.yaw());
        double pitch = Math.toRadians(rotation.pitch());
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return new Vec3(x, y, z).normalize();
    }
}
