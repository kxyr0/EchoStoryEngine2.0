package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationSurfaceRouteService;

public record NavigationAgentProfile(
        double width,
        double height,
        double eyeHeight,
        double maximumStepUp,
        double maximumControlledDrop,
        double movementSpeed,
        boolean canUseStairs,
        boolean canUseSlabs,
        boolean canUseLadders,
        boolean canOpenWoodenDoors,
        boolean canPassOpenDoors,
        long revision) {
    public static NavigationAgentProfile from(NavigationAgentHandle handle) {
        return new NavigationAgentProfile(handle.width(), handle.height(), handle.mob().getEyeHeight(),
                1.0D, 1.25D, 0.16D, true, true, true, true, true, 1L);
    }

    public static NavigationAgentProfile from(NavigationSurfaceRouteService.AgentProfile profile) {
        return new NavigationAgentProfile(profile.width(), profile.height(), profile.eyeHeight(),
                profile.maximumStepUp(), profile.maximumControlledDrop(), profile.movementSpeed(), true, true,
                profile.canUseLadders(), profile.canOpenWoodenDoors(), true, profile.revision());
    }

    public int maximumStepUp16() {
        return (int) Math.floor(maximumStepUp * 16.0D + 0.0001D);
    }

    public int maximumControlledDrop16() {
        return (int) Math.floor(maximumControlledDrop * 16.0D + 0.0001D);
    }
}
