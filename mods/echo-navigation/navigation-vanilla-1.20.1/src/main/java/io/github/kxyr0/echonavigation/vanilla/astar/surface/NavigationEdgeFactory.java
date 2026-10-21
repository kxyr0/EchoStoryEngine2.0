package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalType;
import io.github.kxyr0.echonavigation.core.astar.AStarGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;

final class NavigationEdgeFactory {
    private final NavigationSurfaceSampler sampler;
    private final NavigationAgentProfile profile;

    NavigationEdgeFactory(NavigationSurfaceSampler sampler, NavigationAgentProfile profile) {
        this.sampler = sampler;
        this.profile = profile;
    }

    List<AStarGraph.Step> horizontalSteps(NavigationRouteNode node, Columns columns) {
        List<AStarGraph.Step> result = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (NavigationRouteNode target : columns.nodes(node.blockX() + direction.getStepX(),
                    node.blockZ() + direction.getStepZ())) {
                create(node, target).ifPresent(result::add);
            }
            for (NavigationRouteNode target : columns.nodes(node.blockX() + direction.getStepX() * 2,
                    node.blockZ() + direction.getStepZ() * 2)) {
                createJump(node, target).ifPresent(result::add);
            }
        }
        return ordered(result);
    }

    Optional<AStarGraph.Step> ladderStep(NavigationRouteNode from, NavigationRouteNode to) {
        if (!profile.canUseLadders() || from.blockX() != to.blockX() || from.blockZ() != to.blockZ()) {
            return Optional.empty();
        }
        int delta = to.height16() - from.height16();
        if (Math.abs(delta) != 16 || (!sampler.climbable(from) && !sampler.climbable(to))) {
            return Optional.empty();
        }
        NavigationTraversalType type = delta > 0 ? NavigationTraversalType.LADDER_UP
                : NavigationTraversalType.LADDER_DOWN;
        return Optional.of(new AStarGraph.Step(to, type, from.height16(), to.height16(), 3.0D));
    }

    private Optional<AStarGraph.Step> create(NavigationRouteNode from, NavigationRouteNode to) {
        if (!sampler.sweepClear(from, to)) {
            return Optional.empty();
        }
        int delta = to.height16() - from.height16();
        if (delta > profile.maximumStepUp16() || -delta > profile.maximumControlledDrop16()) {
            return Optional.empty();
        }
        NavigationTraversalType type = traversalType(from, to, delta);
        if (!allowed(type)) {
            return Optional.empty();
        }
        return Optional.of(new AStarGraph.Step(to, type, from.height16(), to.height16(), cost(type, delta)));
    }

    private Optional<AStarGraph.Step> createJump(NavigationRouteNode from, NavigationRouteNode to) {
        if (!sampler.sweepClear(from, to) || doorBetween(from, to)) {
            return Optional.empty();
        }
        int delta = to.height16() - from.height16();
        if (delta > profile.maximumStepUp16() || -delta > profile.maximumControlledDrop16()) {
            return Optional.empty();
        }
        return Optional.of(new AStarGraph.Step(to, NavigationTraversalType.JUMP,
                from.height16(), to.height16(), cost(NavigationTraversalType.JUMP, delta)));
    }

    private boolean doorBetween(NavigationRouteNode from, NavigationRouteNode to) {
        int dx = to.blockX() - from.blockX();
        int dz = to.blockZ() - from.blockZ();
        if (Math.abs(dx) + Math.abs(dz) != 2 || dx != 0 && dz != 0) {
            return false;
        }
        int x = from.blockX() + Integer.signum(dx);
        int z = from.blockZ() + Integer.signum(dz);
        int y = Math.max(from.blockY(), to.blockY());
        return sampler.doorTraversal(new NavigationRouteNode(x, y, z, 0)).isPresent();
    }

    private NavigationTraversalType traversalType(NavigationRouteNode from, NavigationRouteNode to, int delta) {
        Optional<NavigationTraversalType> door = sampler.doorTraversal(to);
        if (door.isPresent()) {
            return door.orElseThrow();
        }
        boolean stairLike = sampler.stairLike(from) || sampler.stairLike(to);
        if (delta == 0) {
            return NavigationTraversalType.WALK;
        }
        if (Math.abs(delta) == 8) {
            return delta > 0 ? NavigationTraversalType.SLAB_UP : NavigationTraversalType.SLAB_DOWN;
        }
        if (stairLike || Math.abs(delta) < 16) {
            return delta > 0 ? NavigationTraversalType.STAIR_UP : NavigationTraversalType.STAIR_DOWN;
        }
        return delta > 0 ? NavigationTraversalType.STEP_UP : NavigationTraversalType.STEP_DOWN;
    }

    private boolean allowed(NavigationTraversalType type) {
        return switch (type) {
            case STAIR_UP, STAIR_DOWN -> profile.canUseStairs();
            case SLAB_UP, SLAB_DOWN -> profile.canUseSlabs();
            case OPEN_DOOR -> profile.canOpenWoodenDoors();
            case PASS_DOOR -> profile.canPassOpenDoors();
            case LADDER_UP, LADDER_DOWN -> profile.canUseLadders();
            default -> true;
        };
    }

    private double cost(NavigationTraversalType type, int delta16) {
        double vertical = Math.abs(delta16) / 16.0D;
        return switch (type) {
            case WALK -> 1.0D;
            case SLAB_UP, SLAB_DOWN, STAIR_UP, STAIR_DOWN -> 1.1D + vertical * 0.1D;
            case STEP_UP, STEP_DOWN -> 1.2D + vertical * 0.2D;
            case JUMP -> 2.4D + vertical * 0.5D;
            case OPEN_DOOR -> 3.0D;
            case PASS_DOOR -> 1.1D;
            case LADDER_UP, LADDER_DOWN -> 3.0D + vertical;
        };
    }

    private List<AStarGraph.Step> ordered(List<AStarGraph.Step> steps) {
        return steps.stream().sorted(Comparator.comparing(AStarGraph.Step::target)
                .thenComparing(step -> step.type().ordinal())).toList();
    }

    interface Columns {
        List<NavigationRouteNode> nodes(int x, int z);
    }
}
