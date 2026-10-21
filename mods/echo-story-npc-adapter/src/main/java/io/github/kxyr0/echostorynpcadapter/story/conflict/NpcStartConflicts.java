package io.github.kxyr0.echostorynpcadapter.story.conflict;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echostorynpcadapter.action.SpawnPayload;
import io.github.kxyr0.echostorynpcadapter.action.TeleportPayload;
import java.util.Optional;

public final class NpcStartConflicts {
    public Optional<String> existingMutation(
            NpcService service,
            ActorMutationId mutationId,
            NpcResults.OperationKind kind,
            ActorId actorId) {
        return service.operation(mutationId.value()).operation()
                .filter(operation -> operation.kind() != kind || !operation.actorId().equals(actorId))
                .map(operation -> "ActorMutationId уже занят другой action или ActorId.");
    }

    public Optional<String> spawnActor(NpcService service, SpawnPayload payload) {
        Optional<NpcActorView> actor = service.query(payload.actorId()).actor();
        if (actor.isEmpty()) {
            return Optional.empty();
        }
        if (!actor.orElseThrow().definitionId().equals(payload.definitionId())) {
            return Optional.of("ActorId уже занят другой NpcDefinitionId.");
        }
        return Optional.empty();
    }

    public boolean matchesTransform(NpcActorView actor, SpawnPayload payload) {
        return actor.worldId().equals(payload.worldId())
                && actor.position().equals(payload.position())
                && actor.rotation().equals(payload.rotation());
    }

    public boolean matchesTransform(NpcActorView actor, TeleportPayload payload) {
        return actor.worldId().equals(payload.worldId())
                && actor.position().equals(payload.position())
                && actor.rotation().equals(payload.rotation());
    }
}
