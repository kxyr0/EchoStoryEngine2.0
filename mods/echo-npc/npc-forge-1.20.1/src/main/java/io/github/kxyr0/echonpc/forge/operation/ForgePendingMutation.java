package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable pending mutation context kept only until I/O completion.
 */
public record ForgePendingMutation(
        NpcResults.OperationKind kind,
        ActorDesiredState desired,
        Optional<NpcDefinition> definition,
        Optional<PlatformActorHandle> handle,
        Optional<NpcRestAttachment> releasedRest
) {
    public ForgePendingMutation {
        kind = Objects.requireNonNull(kind, "kind");
        desired = Objects.requireNonNull(desired, "desired");
        definition = Objects.requireNonNull(definition, "definition");
        handle = Objects.requireNonNull(handle, "handle");
        releasedRest = Objects.requireNonNull(releasedRest, "releasedRest");
    }

    public ForgePendingMutation(NpcResults.OperationKind kind, ActorDesiredState desired,
            Optional<NpcDefinition> definition, Optional<PlatformActorHandle> handle) {
        this(kind, desired, definition, handle, Optional.empty());
    }
}
