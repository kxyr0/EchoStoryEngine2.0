package io.github.kxyr0.echomagic.forge.collision;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public record MagicCollisionResult(Optional<BlockHitResult> block, Optional<LivingEntity> entity, Vec3 point) {
    public boolean hit() { return block.isPresent() || entity.isPresent(); }
    public static MagicCollisionResult miss(Vec3 point) {
        return new MagicCollisionResult(Optional.empty(), Optional.empty(), point);
    }
}

