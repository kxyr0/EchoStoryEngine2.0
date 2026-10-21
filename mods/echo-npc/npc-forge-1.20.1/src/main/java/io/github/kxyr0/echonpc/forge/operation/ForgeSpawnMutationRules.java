package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import java.time.Clock;
import java.util.Optional;

/**
 * Applies spawn-specific actor conflict checks and desired-state construction.
 */
public final class ForgeSpawnMutationRules {
    private final ActorRuntime runtime;
    private final Clock clock;

    public ForgeSpawnMutationRules(ActorRuntime runtime, Clock clock) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public Optional<NpcResults.Spawn> existing(SpawnNpcRequest request) {
        Optional<ActiveActor> actor = runtime.actor(request.actorId());
        if (actor.isEmpty()) {
            return Optional.empty();
        }
        if (actor.orElseThrow().desired().definitionId().equals(request.definitionId())) {
            return Optional.of(new NpcResults.Spawn(NpcResults.SpawnStatus.ALREADY_ACTIVE,
                    Optional.of(actor.orElseThrow().view()), "Актёр уже активен."));
        }
        return Optional.of(new NpcResults.Spawn(NpcResults.SpawnStatus.ACTOR_ID_CONFLICT,
                Optional.of(actor.orElseThrow().view()), "ActorId уже занят другой definition."));
    }

    public ActorDesiredState desired(SpawnNpcRequest request, NpcDefinition definition) {
        long now = clock.millis();
        return new ActorDesiredState(request.actorId(), definition.id(), definition.fingerprint(), ActorPresence.ACTIVE,
                request.worldId(), request.position(), request.rotation(), EntityBindingId.generate(), 1L, 0L,
                now, now);
    }
}
