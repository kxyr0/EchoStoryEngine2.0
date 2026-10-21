package io.github.kxyr0.echonpcnavigationadapter.navigation;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

record NpcNavigationAgentHandle(NpcNavigationBody body) implements NavigationAgentHandle {
    NpcNavigationAgentHandle {
        body = Objects.requireNonNull(body, "body");
    }

    @Override
    public NavigationSubjectId subjectId() {
        return NpcSubjectIds.subject(body.actorId());
    }

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
