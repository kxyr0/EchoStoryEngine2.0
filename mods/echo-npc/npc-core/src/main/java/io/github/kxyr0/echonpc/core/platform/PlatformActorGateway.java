package io.github.kxyr0.echonpc.core.platform;

import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import java.util.Objects;
import java.util.Optional;

/**
 * Platform boundary for visible entity operations. Implementations are server-thread synchronous.
 */
public interface PlatformActorGateway {
    boolean worldExists(NpcWorldId worldId);

    boolean chunkLoaded(NpcWorldId worldId, NpcPosition position);

    SpawnResult spawn(ActorDesiredState desired, NpcDefinition definition);

    MoveResult teleport(PlatformActorHandle handle, NpcWorldId worldId, NpcPosition position, NpcRotation rotation);

    RemoveResult despawn(PlatformActorHandle handle);

    boolean exists(PlatformActorHandle handle);

    enum Status {
        ACCEPTED,
        REJECTED,
        MISSING
    }

    record SpawnResult(Status status, Optional<PlatformActorHandle> handle, String message) {
        public SpawnResult {
            status = Objects.requireNonNull(status, "status");
            handle = Objects.requireNonNull(handle, "handle");
            message = Objects.requireNonNull(message, "message");
        }
    }

    record MoveResult(Status status, String message) {
        public MoveResult {
            status = Objects.requireNonNull(status, "status");
            message = Objects.requireNonNull(message, "message");
        }
    }

    record RemoveResult(Status status, String message) {
        public RemoveResult {
            status = Objects.requireNonNull(status, "status");
            message = Objects.requireNonNull(message, "message");
        }
    }
}
