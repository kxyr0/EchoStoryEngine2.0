package io.github.kxyr0.echovisibility.forge.client.runtime;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class ClientTargetBounds {
    public Optional<VisibilityTargetBounds> resolve(
            ClientArmedWatch watch,
            Minecraft minecraft,
            float partialTick) {
        if (!(watch.definition().target() instanceof VisibilityNpcTarget npc)) {
            return watch.stableBounds();
        }
        if (minecraft.level == null || watch.clientEntityId() < 0) {
            return Optional.empty();
        }
        Entity entity = minecraft.level.getEntity(watch.clientEntityId());
        if (entity == null) {
            return Optional.empty();
        }
        double renderX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double renderY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double renderZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        AABB bounds = entity.getBoundingBox().move(renderX - entity.getX(),
                renderY - entity.getY(), renderZ - entity.getZ()).inflate(npc.boundsPadding());
        VisibilityAabb converted = new VisibilityAabb(
                new VisibilityVector3(bounds.minX, bounds.minY, bounds.minZ),
                new VisibilityVector3(bounds.maxX, bounds.maxY, bounds.maxZ));
        return Optional.of(new VisibilityTargetBounds(
                new VisibilityDimensionId(minecraft.level.dimension().location().toString()),
                converted, watch.definition().revision()));
    }
}
