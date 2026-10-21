package io.github.kxyr0.echonavigation.vanilla.astar.motor;

import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class AStarLadderMotor {
    private static final double CENTERING_STEP = 0.10D;
    private static final double VERTICAL_STEP = 0.12D;
    private boolean gravityOverridden;
    private boolean previousNoGravity;

    Vec3 move(NavigationAgentHandle handle, NavigationRouteNode from, NavigationRouteNode target) {
        Mob mob = handle.mob();
        BlockPos targetBlock = new BlockPos(target.blockX(), target.blockY(), target.blockZ());
        BlockPos fromBlock = new BlockPos(from.blockX(), from.blockY(), from.blockZ());
        overrideGravity(mob);
        boolean exit = climbable(handle, fromBlock) && !climbable(handle, targetBlock);
        BlockPos anchor = exit ? fromBlock : targetBlock;
        double targetX = target.blockX() + 0.5D;
        double targetZ = target.blockZ() + 0.5D;
        double probeX = approach(mob.getX(), targetX, CENTERING_STEP);
        double probeZ = approach(mob.getZ(), targetZ, CENTERING_STEP);
        double nextY = exit || readyToClimb(handle, mob, targetBlock, probeX, probeZ)
                ? approach(mob.getY(), target.feetY(), VERTICAL_STEP) : mob.getY();
        double holdX = anchor.getX() + 0.5D;
        double holdZ = anchor.getZ() + 0.5D;
        double nextX = approach(mob.getX(), exitReady(nextY, target.feetY()) ? targetX : holdX, CENTERING_STEP);
        double nextZ = approach(mob.getZ(), exitReady(nextY, target.feetY()) ? targetZ : holdZ, CENTERING_STEP);
        Vec3 movement = new Vec3(nextX - mob.getX(), nextY - mob.getY(), nextZ - mob.getZ());
        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(targetX, target.feetY(), targetZ, 1.0D);
        mob.setPos(nextX, nextY, nextZ);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.resetFallDistance();
        mob.hasImpulse = true;
        return movement;
    }

    boolean controls(NavigationAgentHandle handle, NavigationRouteNode from, NavigationRouteNode to) {
        return climbable(handle, block(from)) || climbable(handle, block(to));
    }

    void release(Mob mob) {
        if (gravityOverridden) {
            mob.setNoGravity(previousNoGravity);
            gravityOverridden = false;
        }
    }

    private void overrideGravity(Mob mob) {
        if (!gravityOverridden) {
            previousNoGravity = mob.isNoGravity();
            mob.setNoGravity(true);
            gravityOverridden = true;
        }
    }

    private double approach(double value, double target, double maxStep) {
        return value + Mth.clamp(target - value, -maxStep, maxStep);
    }

    private boolean readyToClimb(NavigationAgentHandle handle, Mob mob, BlockPos target, double nextX, double nextZ) {
        return centered(target, nextX, nextZ) || touchesCurrentClimbable(handle, mob);
    }

    private boolean centered(BlockPos target, double x, double z) {
        return Math.abs(x - (target.getX() + 0.5D)) <= 0.20D
                && Math.abs(z - (target.getZ() + 0.5D)) <= 0.20D;
    }

    private boolean exitReady(double y, double targetY) {
        return Math.abs(y - targetY) <= 0.02D;
    }

    private boolean touchesCurrentClimbable(NavigationAgentHandle handle, Mob mob) {
        BlockPos current = BlockPos.containing(mob.getX(), mob.getY() + 0.1D, mob.getZ());
        return climbable(handle, current) || climbable(handle, current.above()) || climbable(handle, current.below())
                || climbable(handle, current.north()) || climbable(handle, current.south())
                || climbable(handle, current.east()) || climbable(handle, current.west());
    }

    private boolean climbable(NavigationAgentHandle handle, BlockPos pos) {
        BlockState state = handle.level().getBlockState(pos);
        return state.is(BlockTags.CLIMBABLE);
    }

    private BlockPos block(NavigationRouteNode node) {
        return new BlockPos(node.blockX(), node.blockY(), node.blockZ());
    }
}
