package io.github.kxyr0.echonavigation.vanilla.validation;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import net.minecraft.core.BlockPos;

public final class VanillaArrivalVerifier {
    public boolean arrived(
            NavigationAgentHandle handle,
            NavigationDestination destination,
            NavigationSettingsSnapshot settings) {
        Vector3 target = destination.standingPosition();
        Vector3 current = handle.position();
        BlockPos feet = BlockPos.containing(handle.mob().getX(), handle.mob().getY(), handle.mob().getZ());
        boolean sameFeet = feet.getX() == destination.blockX()
                && feet.getY() == destination.blockY()
                && feet.getZ() == destination.blockZ();
        double vertical = Math.abs(current.y() - target.y());
        return sameFeet && current.horizontalDistanceTo(target) <= settings.arrivalTolerance() && vertical <= 0.75D;
    }
}
