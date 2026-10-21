package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

public final class VanillaLadderClimber {
    private static final double CENTERING_SPEED = 0.08D;
    private static final double ASCEND_SPEED = 0.22D;
    private static final double DESCEND_SPEED = -0.14D;

    public void tick(NavigationAgentHandle handle) {
        Path path = handle.mob().getNavigation().getPath();
        if (path == null || path.isDone()) {
            return;
        }
        BlockPos current = handle.mob().blockPosition();
        BlockPos next = path.getNextNodePos();
        if (!sameColumn(current, next) || !touchesClimbable(handle, current, next)) {
            return;
        }
        climbToward(handle.mob(), next);
    }

    private boolean sameColumn(BlockPos current, BlockPos next) {
        return current.getX() == next.getX() && current.getZ() == next.getZ()
                && current.getY() != next.getY();
    }

    private boolean touchesClimbable(NavigationAgentHandle handle, BlockPos current, BlockPos next) {
        return climbable(handle, current) || climbable(handle, current.above()) || climbable(handle, current.below())
                || climbable(handle, next) || climbable(handle, next.above()) || climbable(handle, next.below());
    }

    private boolean climbable(NavigationAgentHandle handle, BlockPos pos) {
        BlockState state = handle.level().getBlockState(pos);
        return state.is(BlockTags.CLIMBABLE);
    }

    private void climbToward(Mob mob, BlockPos next) {
        double centerX = next.getX() + 0.5D;
        double centerZ = next.getZ() + 0.5D;
        double x = Mth.clamp((centerX - mob.getX()) * 0.35D, -CENTERING_SPEED, CENTERING_SPEED);
        double z = Mth.clamp((centerZ - mob.getZ()) * 0.35D, -CENTERING_SPEED, CENTERING_SPEED);
        double y = next.getY() > mob.getY() ? ASCEND_SPEED : DESCEND_SPEED;
        mob.getMoveControl().setWantedPosition(centerX, next.getY(), centerZ, 1.0D);
        mob.setDeltaMovement(x, y, z);
        mob.resetFallDistance();
        mob.hasImpulse = true;
    }
}
