package io.github.kxyr0.echonpc.core.actor;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import java.util.Objects;
import java.util.Optional;

public record ActiveActor(
        ActorDesiredState desired,
        Optional<NpcDefinition> definition,
        Optional<PlatformActorHandle> handle,
        NpcActorView.Status status,
        Optional<io.github.kxyr0.echonpc.api.spatial.NpcPosition> materializedPosition,
        Optional<String> blockedReason
) {
    public ActiveActor {
        desired = Objects.requireNonNull(desired, "desired");
        definition = Objects.requireNonNull(definition, "definition");
        handle = Objects.requireNonNull(handle, "handle");
        status = Objects.requireNonNull(status, "status");
        materializedPosition = Objects.requireNonNull(materializedPosition, "materializedPosition");
        blockedReason = Objects.requireNonNull(blockedReason, "blockedReason");
    }

    ActorId actorId() {
        return desired.actorId();
    }

    ActiveActor desired(ActorDesiredState state, Optional<NpcDefinition> resolved,
            NpcActorView.Status nextStatus, Optional<String> reason) {
        return new ActiveActor(state, resolved, handle, nextStatus, Optional.empty(), reason);
    }

    ActiveActor materialized(PlatformActorHandle nextHandle) {
        NpcActorView.Status nextStatus = desired.restAttachment().isPresent()
                ? NpcActorView.Status.NPC_SLEEPING : NpcActorView.Status.MATERIALIZED;
        return new ActiveActor(desired, definition, Optional.of(nextHandle), nextStatus,
                Optional.of(desired.position()), Optional.empty());
    }

    ActiveActor detached(NpcActorView.Status nextStatus, Optional<String> reason) {
        return new ActiveActor(desired, definition, Optional.empty(), nextStatus, Optional.empty(), reason);
    }

    public NpcActorView view() {
        NpcDefinition resolved = definition.orElse(null);
        return new NpcActorView(desired.actorId(), desired.definitionId(), displayName(resolved),
                resolved == null ? Optional.empty() : resolved.nameColor(),
                desired.activeSkinId().or(() -> resolved == null ? Optional.empty() : resolved.skinId()),
                desired.activeFormId(),
                desired.activeFormRevision(), desired.activeFormFingerprint(), desired.worldId(), desired.position(),
                desired.rotation(), status, presence(), compatibility(), materializedPosition,
                desired.restAttachment().map(io.github.kxyr0.echonpc.core.rest.NpcRestAttachment::view),
                desired.actorRevision(), desired.repositoryRevision(), blockedReason);
    }

    private String displayName(NpcDefinition resolved) {
        return resolved == null ? "Определение недоступно" : resolved.displayName();
    }

    private NpcActorView.DesiredPresence presence() {
        return desired.presence() == ActorPresence.ACTIVE
                ? NpcActorView.DesiredPresence.ACTIVE
                : NpcActorView.DesiredPresence.ABSENT;
    }

    private NpcActorView.DefinitionCompatibility compatibility() {
        if (status == NpcActorView.Status.BLOCKED_DEFINITION_MISSING) {
            return NpcActorView.DefinitionCompatibility.MISSING;
        }
        if (status == NpcActorView.Status.BLOCKED_DEFINITION_INCOMPATIBLE) {
            return NpcActorView.DefinitionCompatibility.INCOMPATIBLE;
        }
        return definition.isPresent()
                ? NpcActorView.DefinitionCompatibility.COMPATIBLE
                : NpcActorView.DefinitionCompatibility.UNKNOWN;
    }
}
