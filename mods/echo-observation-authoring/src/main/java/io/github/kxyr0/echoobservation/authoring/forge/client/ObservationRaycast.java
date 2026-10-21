package io.github.kxyr0.echoobservation.authoring.forge.client;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class ObservationRaycast {
    private static final double MAX_DISTANCE = 64.0D;

    Optional<ObservationAnchor> cast(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return Optional.empty();
        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        var look = camera.getLookVector();
        Vec3 end = start.add(look.x() * MAX_DISTANCE, look.y() * MAX_DISTANCE, look.z() * MAX_DISTANCE);
        HitResult result = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, minecraft.player));
        if (!(result instanceof BlockHitResult hit) || result.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        var block = hit.getBlockPos();
        Vec3 point = hit.getLocation();
        return Optional.of(new ObservationAnchor(minecraft.level.dimension().location().toString(),
                block.getX(), block.getY(), block.getZ(), unit(point.x - block.getX()),
                unit(point.y - block.getY()), unit(point.z - block.getZ()),
                ObservationFace.valueOf(hit.getDirection().getName().toUpperCase(Locale.ROOT))));
    }

    private double unit(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
