package io.github.kxyr0.echoobservation.forge.client.visibility;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ObservationOcclusion {
    public boolean visible(ObservationAnchor anchor, VisibilityCameraSnapshot camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.level.hasChunk(anchor.blockX() >> 4, anchor.blockZ() >> 4)) {
            return false;
        }
        Vec3 start = new Vec3(camera.position().x(), camera.position().y(), camera.position().z());
        Vec3 end = new Vec3(anchor.worldX(), anchor.worldY(), anchor.worldZ());
        BlockHitResult hit = minecraft.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        if (hit.getType() == HitResult.Type.MISS) return true;
        BlockPos authored = new BlockPos(anchor.blockX(), anchor.blockY(), anchor.blockZ());
        return hit.getBlockPos().equals(authored) || hit.getLocation().distanceToSqr(end) <= 0.01D;
    }
}
