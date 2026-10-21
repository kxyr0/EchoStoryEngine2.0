package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public final class VanillaBlockGridFollower {
    private static final int LOOKAHEAD_NODES = 5;
    private static final double APPROACH_DISTANCE_SQR = 2.56D;
    private static final double CENTERING_SPEED = 0.18D;
    private static final double DESCEND_SPEED = -0.12D;

    public void tick(NavigationAgentHandle handle) {
        Path path = handle.mob().getNavigation().getPath();
        if (path == null || path.isDone()) {
            return;
        }
        nextDownStep(path).filter(step -> near(handle.mob(), step.from()))
                .ifPresent(step -> descend(handle.mob(), path, step));
    }

    static Optional<DownStep> nextDownStep(Path path) {
        int first = Math.max(1, path.getNextNodeIndex());
        int end = Math.min(path.getNodeCount(), first + LOOKAHEAD_NODES);
        for (int index = first; index < end; index++) {
            BlockPos from = pos(path.getNode(index - 1));
            BlockPos to = pos(path.getNode(index));
            if (to.getY() < from.getY() && adjacentColumn(from, to)) {
                return Optional.of(new DownStep(index - 1, index, from, to));
            }
        }
        return Optional.empty();
    }

    private static boolean adjacentColumn(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        return dx + dz == 1;
    }

    private static BlockPos pos(Node node) {
        return new BlockPos(node.x, node.y, node.z);
    }

    private boolean near(Mob mob, BlockPos from) {
        double dx = mob.getX() - (from.getX() + 0.5D);
        double dz = mob.getZ() - (from.getZ() + 0.5D);
        return dx * dx + dz * dz <= APPROACH_DISTANCE_SQR && Math.abs(mob.getY() - from.getY()) <= 1.25D;
    }

    private void descend(Mob mob, Path path, DownStep step) {
        path.setNextNodeIndex(step.toIndex());
        double x = step.to().getX() + 0.5D;
        double z = step.to().getZ() + 0.5D;
        double dx = Mth.clamp((x - mob.getX()) * 0.45D, -CENTERING_SPEED, CENTERING_SPEED);
        double dz = Mth.clamp((z - mob.getZ()) * 0.45D, -CENTERING_SPEED, CENTERING_SPEED);
        Vec3 movement = mob.getDeltaMovement();
        mob.getMoveControl().setWantedPosition(x, step.to().getY(), z, 1.0D);
        if (mob.getY() > step.to().getY() + 0.2D) {
            mob.setDeltaMovement(dx, Math.min(movement.y, DESCEND_SPEED), dz);
            mob.resetFallDistance();
            mob.hasImpulse = true;
        }
    }

    record DownStep(int fromIndex, int toIndex, BlockPos from, BlockPos to) {
    }
}
