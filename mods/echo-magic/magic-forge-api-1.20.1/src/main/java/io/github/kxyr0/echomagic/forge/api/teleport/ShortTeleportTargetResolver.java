package io.github.kxyr0.echomagic.forge.api.teleport;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShortTeleportTargetResolver {
    public static final double DEFAULT_RANGE = 6.0D;
    private static final double DISTANCE_STEP = 0.25D;
    private static final double MINIMUM_DISTANCE = 1.0D;

    private ShortTeleportTargetResolver() { }

    public static Optional<Vec3> find(Level level, LivingEntity entity, double requestedRange) {
        if (level == null || entity == null || !Double.isFinite(requestedRange)
                || requestedRange < MINIMUM_DISTANCE) return Optional.empty();
        Vec3 look = entity.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-6D) return Optional.empty();
        direction = direction.normalize();
        double range = unobstructedRange(level, entity, direction, requestedRange);
        for (double distance = range; distance >= MINIMUM_DISTANCE; distance -= DISTANCE_STEP) {
            Optional<Vec3> landing = landing(level, entity, entity.position().add(direction.scale(distance)));
            if (landing.isPresent()) return landing;
        }
        return Optional.empty();
    }

    private static double unobstructedRange(Level level, LivingEntity entity, Vec3 direction,
                                            double requestedRange) {
        Vec3 start = entity.getEyePosition();
        Vec3 end = start.add(direction.scale(requestedRange));
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK) return requestedRange;
        double clearance = Math.max(0.35D, entity.getBbWidth() * 0.5D + 0.2D);
        return Math.max(0.0D, start.distanceTo(hit.getLocation()) - clearance);
    }

    private static Optional<Vec3> landing(Level level, LivingEntity entity, Vec3 horizontal) {
        int baseY = Mth.floor(entity.getY());
        int blockX = Mth.floor(horizontal.x);
        int blockZ = Mth.floor(horizontal.z);
        for (int floorY = baseY; floorY >= baseY - 3; floorY--) {
            BlockPos floor = new BlockPos(blockX, floorY, blockZ);
            BlockState state = level.getBlockState(floor);
            VoxelShape shape = state.getCollisionShape(level, floor);
            if (shape.isEmpty()) continue;
            double destinationY = floorY + shape.max(Direction.Axis.Y);
            Vec3 destination = new Vec3(horizontal.x, destinationY, horizontal.z);
            if (free(level, entity, destination)) return Optional.of(destination);
        }
        return Optional.empty();
    }

    private static boolean free(Level level, LivingEntity entity, Vec3 destination) {
        BlockPos pos = BlockPos.containing(destination);
        if (!level.hasChunkAt(pos)) return false;
        AABB bounds = entity.getBoundingBox().move(destination.subtract(entity.position()));
        return level.getWorldBorder().isWithinBounds(bounds)
                && !level.getBlockCollisions(entity, bounds).iterator().hasNext()
                && level.getEntities(entity, bounds, other -> other.isAlive()
                        && other.canBeCollidedWith()).isEmpty();
    }
}
