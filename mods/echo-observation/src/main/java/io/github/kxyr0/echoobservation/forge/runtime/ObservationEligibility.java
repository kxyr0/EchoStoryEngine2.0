package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.core.progress.ObservationPlayerProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class ObservationEligibility {
    boolean focusable(ServerPlayer player, ObservationDefinition definition, ObservationPlayerProgress progress) {
        if (definition.anchor().isEmpty() || !progress.active(definition.id().resource())
                || !progress.enabled(definition.id())
                || progress.completed(definition.id()) && !definition.repeatable()) {
            return false;
        }
        var anchor = definition.anchor().orElseThrow();
        if (!player.level().dimension().location().toString().equals(anchor.dimension())) {
            return false;
        }
        double distanceSquared = player.position().distanceToSqr(
                anchor.worldX(), anchor.worldY(), anchor.worldZ());
        double maximum = definition.distance() + 1.25D;
        return distanceSquared <= maximum * maximum && aimedAndVisible(player, definition);
    }

    private boolean aimedAndVisible(ServerPlayer player, ObservationDefinition definition) {
        var anchor = definition.anchor().orElseThrow();
        Vec3 eye = player.getEyePosition();
        Vec3 target = new Vec3(anchor.worldX(), anchor.worldY(), anchor.worldZ());
        Vec3 delta = target.subtract(eye);
        if (delta.lengthSqr() < 0.000001D
                || player.getLookAngle().dot(delta.normalize()) < Math.cos(Math.toRadians(10))) {
            return false;
        }
        HitResult result = player.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return result.getType() == HitResult.Type.MISS || result instanceof BlockHitResult hit
                && hit.getBlockPos().getX() == anchor.blockX()
                && hit.getBlockPos().getY() == anchor.blockY()
                && hit.getBlockPos().getZ() == anchor.blockZ();
    }
}
