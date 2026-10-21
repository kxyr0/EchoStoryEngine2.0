package io.github.kxyr0.echonavigation.vanilla.validation;

import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class VanillaDestinationValidator {
    public Optional<NavigationFailure> validate(NavigationAgentHandle handle, NavigationDestination destination) {
        if (!handle.dimensionId().equals(destination.dimensionId())) {
            return Optional.of(NavigationFailure.NAVIGATION_SUBJECT_WRONG_DIMENSION);
        }
        ServerLevel level = handle.level();
        BlockPos feet = new BlockPos(destination.blockX(), destination.blockY(), destination.blockZ());
        if (!level.hasChunkAt(feet)) {
            return Optional.of(NavigationFailure.NPC_MOVE_DESTINATION_UNLOADED);
        }
        if (!standable(level, handle.mob(), feet)) {
            return Optional.of(NavigationFailure.NPC_MOVE_DESTINATION_NOT_STANDABLE);
        }
        return Optional.empty();
    }

    private boolean standable(ServerLevel level, Mob mob, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
            return false;
        }
        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
            return false;
        }
        if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
            return false;
        }
        double halfWidth = Math.max(0.15D, mob.getBbWidth() / 2.0D);
        AABB box = new AABB(feet.getX() + 0.5D - halfWidth, feet.getY(), feet.getZ() + 0.5D - halfWidth,
                feet.getX() + 0.5D + halfWidth, feet.getY() + mob.getBbHeight(),
                feet.getZ() + 0.5D + halfWidth);
        return level.noCollision(mob, box);
    }
}
