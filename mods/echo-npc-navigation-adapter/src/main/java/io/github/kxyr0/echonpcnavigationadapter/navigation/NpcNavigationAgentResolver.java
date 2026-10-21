package io.github.kxyr0.echonpcnavigationadapter.navigation;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolution;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolver;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupResult;
import net.minecraft.server.MinecraftServer;

public final class NpcNavigationAgentResolver implements NavigationAgentResolver {
    @Override
    public NavigationAgentResolution resolve(MinecraftServer server, NavigationSubjectId subjectId) {
        NpcNavigationBodyLookupResult result = EchoNpcNavigationBodyBridge.lookup(server, NpcSubjectIds.actor(subjectId));
        return switch (result.status()) {
            case RESOLVED -> NavigationAgentResolution.resolved(
                    new NpcNavigationAgentHandle(result.body().orElseThrow()));
            case NOT_MATERIALIZED -> NavigationAgentResolution.missing(
                    NavigationAgentResolution.Status.NOT_MATERIALIZED, result.message());
            default -> NavigationAgentResolution.missing(
                    NavigationAgentResolution.Status.SUBJECT_NOT_FOUND, result.message());
        };
    }
}
