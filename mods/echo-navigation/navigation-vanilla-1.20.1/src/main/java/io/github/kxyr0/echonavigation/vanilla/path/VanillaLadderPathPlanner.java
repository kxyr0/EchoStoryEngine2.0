package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public final class VanillaLadderPathPlanner {
    private static final int HORIZONTAL_MARGIN = 24;
    private static final int VERTICAL_MARGIN = 6;
    private static final int MAX_VISITED = 8192;

    public Optional<Path> plan(NavigationAgentHandle handle, NavigationDestination destination) {
        BlockPos start = handle.mob().blockPosition();
        BlockPos target = new BlockPos(destination.blockX(), destination.blockY(), destination.blockZ());
        Bounds bounds = Bounds.around(start, target);
        PriorityQueue<Entry> open = new PriorityQueue<>(Comparator.comparingDouble(Entry::score));
        Map<BlockPos, Entry> entries = new HashMap<>();
        Entry first = new Entry(start, null, 0.0D, distance(start, target));
        open.add(first);
        entries.put(start, first);
        int visited = 0;
        while (!open.isEmpty() && visited++ < MAX_VISITED) {
            Entry current = open.poll();
            if (current.closed() || entries.get(current.pos()) != current) {
                continue;
            }
            current.close();
            if (current.pos().equals(target)) {
                return Optional.of(path(handle, current, target));
            }
            for (BlockPos next : neighbors(handle, current.pos(), bounds)) {
                double cost = current.cost() + moveCost(handle.level(), current.pos(), next);
                Entry known = entries.get(next);
                if (known == null || cost < known.cost()) {
                    Entry updated = new Entry(next, current, cost, cost + distance(next, target));
                    entries.put(next, updated);
                    open.add(updated);
                }
            }
        }
        return Optional.empty();
    }

    private List<BlockPos> neighbors(NavigationAgentHandle handle, BlockPos pos, Bounds bounds) {
        List<BlockPos> result = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            addWalkCandidate(handle, bounds, result, pos.relative(direction));
            addWalkCandidate(handle, bounds, result, pos.relative(direction).above());
            addWalkCandidate(handle, bounds, result, pos.relative(direction).below());
        }
        addLadderCandidate(handle, bounds, result, pos, pos.above());
        addLadderCandidate(handle, bounds, result, pos, pos.below());
        return result;
    }

    private void addWalkCandidate(NavigationAgentHandle handle, Bounds bounds, List<BlockPos> result, BlockPos pos) {
        if (!bounds.contains(pos) || !loaded(handle.level(), pos)) {
            return;
        }
        if (occupiable(handle.level(), handle.mob(), pos)
                && (supported(handle.level(), pos) || climbable(handle.level(), pos))) {
            result.add(pos);
        }
    }

    private void addLadderCandidate(NavigationAgentHandle handle, Bounds bounds, List<BlockPos> result,
            BlockPos from, BlockPos to) {
        if (bounds.contains(to) && loaded(handle.level(), to) && occupiable(handle.level(), handle.mob(), to)
                && climbable(handle.level(), from) && climbable(handle.level(), to)) {
            result.add(to);
        }
    }

    private boolean occupiable(ServerLevel level, Mob mob, BlockPos feet) {
        int height = Math.max(2, (int) Math.ceil(mob.getBbHeight()));
        for (int offset = 0; offset < height; offset++) {
            BlockPos pos = feet.above(offset);
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
            if (!state.getCollisionShape(level, pos).isEmpty() && !woodenDoor(state) && !state.is(BlockTags.CLIMBABLE)) {
                return false;
            }
        }
        return true;
    }

    private boolean supported(ServerLevel level, BlockPos feet) {
        return !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private boolean climbable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.CLIMBABLE);
    }

    private boolean loaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.hasChunkAt(pos.above());
    }

    private boolean woodenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(state);
    }

    private double moveCost(ServerLevel level, BlockPos from, BlockPos to) {
        double cost = from.distManhattan(to);
        return woodenDoor(level.getBlockState(to)) ? cost + 2.0D : cost;
    }

    private double distance(BlockPos from, BlockPos to) {
        return from.distManhattan(to);
    }

    private Path path(NavigationAgentHandle handle, Entry end, BlockPos target) {
        List<BlockPos> positions = new ArrayList<>();
        for (Entry entry = end; entry != null; entry = entry.previous()) {
            positions.add(0, entry.pos());
        }
        List<Node> nodes = new ArrayList<>();
        for (BlockPos pos : positions) {
            Node node = new Node(pos.getX(), pos.getY(), pos.getZ());
            node.type = woodenDoor(handle.level().getBlockState(pos)) ? BlockPathTypes.WALKABLE_DOOR
                    : BlockPathTypes.WALKABLE;
            node.costMalus = 0.0F;
            nodes.add(node);
        }
        return new Path(nodes, target, true);
    }

    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static Bounds around(BlockPos start, BlockPos target) {
            return new Bounds(
                    Math.min(start.getX(), target.getX()) - HORIZONTAL_MARGIN,
                    Math.max(start.getX(), target.getX()) + HORIZONTAL_MARGIN,
                    Math.min(start.getY(), target.getY()) - VERTICAL_MARGIN,
                    Math.max(start.getY(), target.getY()) + VERTICAL_MARGIN,
                    Math.min(start.getZ(), target.getZ()) - HORIZONTAL_MARGIN,
                    Math.max(start.getZ(), target.getZ()) + HORIZONTAL_MARGIN);
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }

    private static final class Entry {
        private final BlockPos pos;
        private final Entry previous;
        private final double cost;
        private final double score;
        private boolean closed;

        Entry(BlockPos pos, Entry previous, double cost, double score) {
            this.pos = pos;
            this.previous = previous;
            this.cost = cost;
            this.score = score;
        }

        BlockPos pos() {
            return pos;
        }

        Entry previous() {
            return previous;
        }

        double cost() {
            return cost;
        }

        double score() {
            return score;
        }

        boolean closed() {
            return closed;
        }

        void close() {
            closed = true;
        }
    }

}
