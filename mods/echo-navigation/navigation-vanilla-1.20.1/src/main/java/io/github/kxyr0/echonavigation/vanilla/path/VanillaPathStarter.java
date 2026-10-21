package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public final class VanillaPathStarter {
    static final int EXACT_TARGET_ACCURACY = 0;
    static final int RELAXED_TARGET_ACCURACY = 1;

    public Result start(NavigationAgentHandle handle, NavigationDestination destination, double speedModifier) {
        BlockPos target = new BlockPos(destination.blockX(), destination.blockY(), destination.blockZ());
        return startToBlock(handle, target, speedModifier, "Vanilla path started.");
    }

    public Result startToBlock(NavigationAgentHandle handle, BlockPos target, double speedModifier, String message) {
        PathNavigation navigation = handle.mob().getNavigation();
        Path path = pathToExactTarget(navigation, target);
        if (path == null) {
            return Result.failed(NavigationFailure.NAVIGATION_VANILLA_PATH_NOT_FOUND,
                    "Vanilla path not found.");
        }
        return startPath(handle, path, speedModifier, message);
    }

    private Path pathToExactTarget(PathNavigation navigation, BlockPos target) {
        Path exact = navigation.createPath(target, EXACT_TARGET_ACCURACY);
        if (reachesTarget(exact, target)) {
            return exact;
        }
        Path relaxed = navigation.createPath(target, RELAXED_TARGET_ACCURACY);
        return reachesTarget(relaxed, target) ? relaxed : null;
    }

    static boolean reachesTarget(Path path, BlockPos target) {
        if (path == null || !path.canReach() || path.getNodeCount() == 0) {
            return false;
        }
        var end = path.getNode(path.getNodeCount() - 1);
        return end.x == target.getX() && end.y == target.getY() && end.z == target.getZ();
    }

    public Result startPath(NavigationAgentHandle handle, Path path, double speedModifier, String message) {
        if (!handle.mob().getNavigation().moveTo(path, speedModifier)) {
            return Result.failed(NavigationFailure.NAVIGATION_VANILLA_START_REJECTED,
                    "Vanilla refused moveTo.");
        }
        return new Result(Optional.of(path), Optional.empty(), message);
    }

    public record Result(Optional<Path> path, Optional<NavigationFailure> failure, String message) {
        public Result {
            path = Objects.requireNonNull(path, "path");
            failure = Objects.requireNonNull(failure, "failure");
            message = Objects.requireNonNull(message, "message");
        }

        public static Result failed(NavigationFailure failure, String message) {
            return new Result(Optional.empty(), Optional.of(failure), message);
        }
    }
}
