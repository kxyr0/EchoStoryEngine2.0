package io.github.kxyr0.echonpcbackpack.forge.approach;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolution;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolver;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Resolves Echo NPC actors when the general NPC/navigation adapter is absent. */
public final class BackpackNavigationAgentResolver implements NavigationAgentResolver {
    @Override
    public NavigationAgentResolution resolve(MinecraftServer server, NavigationSubjectId subjectId) {
        var result = EchoNpcNavigationBodyBridge.lookup(server, new ActorId(subjectId.value()));
        return switch (result.status()) {
            case RESOLVED -> NavigationAgentResolution.resolved(new Handle(subjectId, result.body().orElseThrow()));
            case NOT_MATERIALIZED -> NavigationAgentResolution.missing(
                    NavigationAgentResolution.Status.NOT_MATERIALIZED, result.message());
            default -> NavigationAgentResolution.missing(
                    NavigationAgentResolution.Status.SUBJECT_NOT_FOUND, result.message());
        };
    }

    private record Handle(NavigationSubjectId subjectId, NpcNavigationBody body)
            implements NavigationAgentHandle {
        @Override
        public String dimensionId() {
            return body.dimensionId();
        }

        @Override
        public ServerLevel level() {
            return body.level();
        }

        @Override
        public Mob mob() {
            return body.mob();
        }
    }
}
