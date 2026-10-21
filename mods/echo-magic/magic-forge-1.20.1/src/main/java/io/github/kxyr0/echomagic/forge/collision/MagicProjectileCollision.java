package io.github.kxyr0.echomagic.forge.collision;

import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MagicProjectileCollision {
    public MagicCollisionResult resolve(MagicProjectileEntity projectile, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = projectile.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY : start.distanceToSqr(blockHit.getLocation());
        Optional<EntityCandidate> entity = projectile.level().getEntities(projectile,
                        projectile.getBoundingBox().expandTowards(end.subtract(start)).inflate(projectile.payload().radius()),
                        value -> value instanceof LivingEntity living && projectile.validTarget(living)).stream()
                .map(value -> (LivingEntity) value)
                .flatMap(value -> value.getBoundingBox().inflate(projectile.payload().radius()).clip(start, end).stream()
                        .map(point -> new EntityCandidate(value, point, start.distanceToSqr(point))))
                .filter(candidate -> candidate.distance < blockDistance)
                .min(Comparator.comparingDouble(EntityCandidate::distance)
                        .thenComparingInt(candidate -> candidate.entity().getId()));
        if (entity.isPresent()) {
            EntityCandidate hit = entity.orElseThrow();
            return new MagicCollisionResult(Optional.empty(), Optional.of(hit.entity()), hit.point());
        }
        return blockHit.getType() == HitResult.Type.MISS ? MagicCollisionResult.miss(end)
                : new MagicCollisionResult(Optional.of(blockHit), Optional.empty(), blockHit.getLocation());
    }

    private record EntityCandidate(LivingEntity entity, Vec3 point, double distance) { }
}

