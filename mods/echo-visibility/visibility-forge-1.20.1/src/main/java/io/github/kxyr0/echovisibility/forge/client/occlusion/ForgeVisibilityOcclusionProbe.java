package io.github.kxyr0.echovisibility.forge.client.occlusion;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.core.occlusion.VisibilityOcclusionProbe;
import io.github.kxyr0.echovisibility.core.occlusion.VisibilityOcclusionResult;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ForgeVisibilityOcclusionProbe implements VisibilityOcclusionProbe {
    private static final double TARGET_EPSILON = 0.05D;
    private final ClientLevel level;

    public ForgeVisibilityOcclusionProbe(ClientLevel level) {
        this.level = level;
    }

    @Override
    public VisibilityOcclusionResult probe(VisibilityVector3 origin, VisibilityVector3 sample) {
        Vec3 start = vector(origin);
        Vec3 end = vector(sample);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, null));
        if (hit.getType() == HitResult.Type.MISS
                || start.distanceTo(hit.getLocation()) + TARGET_EPSILON >= start.distanceTo(end)) {
            return VisibilityOcclusionResult.clear();
        }
        String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(hit.getBlockPos()).getBlock()).toString();
        String detail = block + " @ " + hit.getBlockPos().toShortString()
                + " face=" + hit.getDirection().getName() + " shape=VISUAL";
        return new VisibilityOcclusionResult(VisibilityOcclusionResult.Status.BLOCKED,
                Optional.of(vector(hit.getLocation())), Optional.of(detail));
    }

    private static Vec3 vector(VisibilityVector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    private static VisibilityVector3 vector(Vec3 value) {
        return new VisibilityVector3(value.x, value.y, value.z);
    }
}
