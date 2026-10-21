package io.github.kxyr0.echobackgroundnpc.forge.api.location;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import net.minecraft.server.MinecraftServer;

/** Exact named-location boundary; nearest/registration-order fallbacks are forbidden. */
@FunctionalInterface
public interface BackgroundLocationResolver {
    BackgroundLocationResolution resolve(MinecraftServer server, BackgroundLocationId id);
}
