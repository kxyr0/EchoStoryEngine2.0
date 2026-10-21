package io.github.kxyr0.echonavigation.forge.api.agent;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Narrow public Forge handle for one materialized navigation-capable body.
 */
public interface NavigationAgentHandle {
    NavigationSubjectId subjectId();

    String dimensionId();

    ServerLevel level();

    Mob mob();

    default Vector3 position() {
        Mob mob = mob();
        return new Vector3(mob.getX(), mob.getY(), mob.getZ());
    }

    default double width() {
        return mob().getBbWidth();
    }

    default double height() {
        return mob().getBbHeight();
    }
}
