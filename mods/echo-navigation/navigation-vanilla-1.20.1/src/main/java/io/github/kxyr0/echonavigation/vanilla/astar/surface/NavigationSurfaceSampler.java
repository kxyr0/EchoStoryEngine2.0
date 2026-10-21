package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NavigationSurfaceSampler {
    private static final double EPSILON = 1.0E-5D;
    private final ServerLevel level;
    private final NavigationAgentProfile profile;

    public NavigationSurfaceSampler(ServerLevel level, NavigationAgentProfile profile) {
        this.level = level;
        this.profile = profile;
    }

    public Optional<NavigationRouteNode> nearestSurface(double x, double y, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        List<NavigationRouteNode> candidates = new ArrayList<>();
        for (int supportY = (int) Math.floor(y) - 2; supportY <= (int) Math.floor(y) + 1; supportY++) {
            candidates.addAll(surfaceNodes(blockX, blockZ, supportY));
            addClimbableNode(candidates, blockX, supportY, blockZ);
        }
        return candidates.stream().min((left, right) ->
                Double.compare(Math.abs(left.feetY() - y), Math.abs(right.feetY() - y)));
    }

    public Optional<NavigationRouteNode> destinationSurface(NavigationDestination destination) {
        List<NavigationRouteNode> candidates = new ArrayList<>();
        int blockY = destination.blockY();
        for (int supportY = blockY - 3; supportY <= blockY + 1; supportY++) {
            candidates.addAll(surfaceNodes(destination.blockX(), destination.blockZ(), supportY));
            addClimbableNode(candidates, destination.blockX(), supportY, destination.blockZ());
        }
        return candidates.stream().min((left, right) -> compareDestination(left, right, blockY));
    }

    public List<NavigationRouteNode> surfaceNodes(int x, int z, int supportY) {
        BlockPos support = new BlockPos(x, supportY, z);
        if (!loaded(support) || lava(support)) {
            return List.of();
        }
        VoxelShape shape = level.getBlockState(support).getCollisionShape(level, support);
        if (shape.isEmpty()) {
            return List.of();
        }
        TreeSet<Integer> heights = new TreeSet<>();
        for (AABB box : shape.toAabbs()) {
            if (coversCenter(box) && box.maxY > EPSILON) {
                heights.add(quantizedHeight16(supportY, box.maxY));
            }
        }
        slabHeight(level.getBlockState(support), supportY).ifPresent(heights::add);
        List<NavigationRouteNode> result = new ArrayList<>();
        for (int height16 : heights) {
            NavigationRouteNode node = node(x, z, height16);
            if (clearance(node)) {
                result.add(node);
            }
        }
        return result;
    }

    public Optional<NavigationRouteNode> climbableNode(int x, int y, int z) {
        List<NavigationRouteNode> result = new ArrayList<>();
        addClimbableNode(result, x, y, z);
        return result.stream().findFirst();
    }

    public boolean clearance(NavigationRouteNode node) {
        return clearanceAt(node.blockX() + 0.5D, node.feetY(), node.blockZ() + 0.5D);
    }

    public boolean clearanceAt(double x, double y, double z) {
        double half = Math.max(0.15D, profile.width() / 2.0D);
        AABB body = new AABB(x - half, y + EPSILON, z - half, x + half,
                y + profile.height() - EPSILON, z + half);
        BlockPos min = BlockPos.containing(body.minX, body.minY, body.minZ);
        BlockPos max = BlockPos.containing(body.maxX, body.maxY, body.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (blocking(pos, body)) {
                return false;
            }
        }
        return true;
    }

    public boolean sweepClear(NavigationRouteNode from, NavigationRouteNode to) {
        return clearance(from) && clearance(to)
                && clearanceAt((from.blockX() + to.blockX() + 1.0D) / 2.0D,
                        Math.max(from.feetY(), to.feetY()), (from.blockZ() + to.blockZ() + 1.0D) / 2.0D);
    }

    public boolean climbable(NavigationRouteNode node) {
        return level.getBlockState(block(node)).is(BlockTags.CLIMBABLE);
    }

    public Optional<NavigationTraversalType> doorTraversal(NavigationRouteNode node) {
        for (BlockPos pos : List.of(block(node), block(node).above(), block(node).below())) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && openOrWoodenDoor(state)) {
                return Optional.of(state.getValue(DoorBlock.OPEN)
                        ? NavigationTraversalType.PASS_DOOR : NavigationTraversalType.OPEN_DOOR);
            }
        }
        return Optional.empty();
    }

    public boolean stairLike(NavigationRouteNode node) {
        BlockPos support = supportBlock(node);
        BlockState state = level.getBlockState(support);
        return state.getBlock() instanceof StairBlock || state.getCollisionShape(level, support).toAabbs().size() > 1;
    }

    public BlockPos block(NavigationRouteNode node) {
        return new BlockPos(node.blockX(), node.blockY(), node.blockZ());
    }

    private void addClimbableNode(List<NavigationRouteNode> result, int x, int y, int z) {
        NavigationRouteNode node = new NavigationRouteNode(x, y, z, 0);
        BlockPos pos = block(node);
        if (loaded(pos) && level.getBlockState(pos).is(BlockTags.CLIMBABLE) && clearance(node)) {
            result.add(node);
        }
    }

    private boolean blocking(BlockPos pos, AABB body) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.LAVA) || !loaded(pos)) {
            return true;
        }
        if (passable(state)) {
            return false;
        }
        for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
            if (intersects(box.move(pos.getX(), pos.getY(), pos.getZ()), body)) {
                return true;
            }
        }
        return false;
    }

    private boolean passable(BlockState state) {
        if (state.getCollisionShape(level, BlockPos.ZERO).isEmpty() || state.is(BlockTags.CLIMBABLE)) {
            return true;
        }
        return state.getBlock() instanceof DoorBlock && openOrWoodenDoor(state);
    }

    private int compareDestination(NavigationRouteNode left, NavigationRouteNode right, int blockY) {
        int byBand = Integer.compare(destinationBand(left, blockY), destinationBand(right, blockY));
        if (byBand != 0) {
            return byBand;
        }
        int byDistance = Double.compare(Math.abs(left.feetY() - blockY), Math.abs(right.feetY() - blockY));
        return byDistance != 0 ? byDistance : left.compareTo(right);
    }

    private int destinationBand(NavigationRouteNode node, int blockY) {
        double feet = node.feetY();
        if (feet >= blockY - EPSILON && feet <= blockY + 1.0D + EPSILON) {
            return 0;
        }
        return feet >= blockY - 1.0D - EPSILON && feet <= blockY + 2.0D + EPSILON ? 1 : 2;
    }

    private Optional<Integer> slabHeight(BlockState state, int supportY) {
        if (!(state.getBlock() instanceof SlabBlock)) {
            return Optional.empty();
        }
        SlabType type = state.getValue(SlabBlock.TYPE);
        return Optional.of(supportY * 16 + (type == SlabType.BOTTOM ? 8 : 16));
    }

    private boolean openOrWoodenDoor(BlockState state) {
        return state.getValue(DoorBlock.OPEN) || profile.canOpenWoodenDoors() && DoorBlock.isWoodenDoor(state);
    }

    private BlockPos supportBlock(NavigationRouteNode node) {
        int absoluteHeight16 = node.height16();
        int supportY = Math.floorDiv(absoluteHeight16 - 1, 16);
        return new BlockPos(node.blockX(), supportY, node.blockZ());
    }

    private NavigationRouteNode node(int x, int z, int absoluteHeight16) {
        return new NavigationRouteNode(x, Math.floorDiv(absoluteHeight16, 16), z,
                Math.floorMod(absoluteHeight16, 16));
    }

    private int quantizedHeight16(int supportY, double localHeight) {
        return supportY * 16 + Math.max(0, Math.min(16, (int) Math.round(localHeight * 16.0D)));
    }

    private boolean coversCenter(AABB box) {
        return box.minX <= 0.5D + EPSILON && box.maxX >= 0.5D - EPSILON
                && box.minZ <= 0.5D + EPSILON && box.maxZ >= 0.5D - EPSILON;
    }

    private boolean intersects(AABB left, AABB right) {
        return left.maxX > right.minX && left.minX < right.maxX
                && left.maxY > right.minY && left.minY < right.maxY
                && left.maxZ > right.minZ && left.minZ < right.maxZ;
    }

    private boolean lava(BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(FluidTags.LAVA);
    }

    private boolean loaded(BlockPos pos) {
        return level.hasChunkAt(pos) && level.hasChunkAt(pos.above());
    }
}
