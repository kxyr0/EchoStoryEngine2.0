package io.github.kxyr0.echonpc.forge.api.navigation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Narrow public body exposed for navigation adapters.
 */
public record NpcNavigationBody(ActorId actorId, ServerLevel level, Mob mob) {
    public NpcNavigationBody {
        actorId = Objects.requireNonNull(actorId, "actorId");
        level = Objects.requireNonNull(level, "level");
        mob = Objects.requireNonNull(mob, "mob");
    }

    public String dimensionId() {
        return level.dimension().location().toString();
    }
}
