package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;

public final class VanillaDoorSupport {
    private static final double OPEN_DISTANCE_SQR = 1.0D;
    private static final int LOOKAHEAD_NODES = 3;

    public void configure(NavigationAgentHandle handle) {
        Mob mob = handle.mob();
        mob.setPathfindingMalus(BlockPathTypes.DOOR_OPEN, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.WALKABLE_DOOR, 0.0F);
        PathNavigation navigation = mob.getNavigation();
        NodeEvaluator evaluator = navigation.getNodeEvaluator();
        evaluator.setCanOpenDoors(true);
        evaluator.setCanPassDoors(true);
        if (navigation instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
    }

    public void openNextDoor(NavigationAgentHandle handle) {
        Path path = handle.mob().getNavigation().getPath();
        if (path == null || path.isDone()) {
            return;
        }
        int end = Math.min(path.getNodeCount(), path.getNextNodeIndex() + LOOKAHEAD_NODES);
        for (int index = path.getNextNodeIndex(); index < end; index++) {
            Node node = path.getNode(index);
            if (tryOpen(handle, new BlockPos(node.x, node.y, node.z))) {
                return;
            }
        }
    }

    public boolean openAround(NavigationAgentHandle handle, BlockPos pathPos) {
        return tryOpen(handle, pathPos);
    }

    private boolean tryOpen(NavigationAgentHandle handle, BlockPos pathPos) {
        for (BlockPos candidate : new BlockPos[] {pathPos, pathPos.above(), pathPos.below()}) {
            if (nearEnough(handle.mob(), candidate) && open(handle.level(), handle.mob(), candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean open(ServerLevel level, Mob mob, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock door) || !DoorBlock.isWoodenDoor(state)
                || state.getValue(DoorBlock.OPEN)) {
            return false;
        }
        BlockPos canonical = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        door.setOpen(mob, level, state, pos, true);
        BlockState canonicalState = level.getBlockState(canonical);
        if (canonicalState.getBlock() instanceof DoorBlock canonicalDoor) {
            canonicalDoor.setOpen(mob, level, canonicalState, canonical, true);
        }
        return true;
    }

    private boolean nearEnough(Mob mob, BlockPos pos) {
        double dx = mob.getX() - (pos.getX() + 0.5D);
        double dy = mob.getY() - pos.getY();
        double dz = mob.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= OPEN_DISTANCE_SQR;
    }
}
