package io.github.kxyr0.echonavigation.forge.api.agent;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import net.minecraft.server.MinecraftServer;

/**
 * Server-thread resolver registered by adapters that own subject identity.
 */
public interface NavigationAgentResolver {
    NavigationAgentResolution resolve(MinecraftServer server, NavigationSubjectId subjectId);
}
