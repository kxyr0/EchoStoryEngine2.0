package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface VisibilityTargetResolver {
    VisibilityTargetResolution resolve(ServerPlayer observer, VisibilityWatchDefinition definition);
}
