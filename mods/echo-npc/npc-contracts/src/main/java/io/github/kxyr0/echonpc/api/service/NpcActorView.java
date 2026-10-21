package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.rest.NpcRestAttachmentView;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;
import java.util.Optional;

/**
 * Public read model for one durable logical actor.
 */
public record NpcActorView(
        ActorId actorId,
        NpcDefinitionId definitionId,
        String displayName,
        Optional<NpcNameColor> nameColor,
        Optional<NpcSkinId> skinId,
        Optional<NpcFormId> activeFormId,
        Optional<NpcFormRevision> activeFormRevision,
        Optional<String> activeFormFingerprint,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation,
        Status status,
        DesiredPresence desiredPresence,
        DefinitionCompatibility definitionCompatibility,
        Optional<NpcPosition> materializedPosition,
        Optional<NpcRestAttachmentView> restAttachment,
        long actorRevision,
        long repositoryRevision,
        Optional<String> blockedReason
) {
    public NpcActorView {
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        displayName = requireDisplay(displayName);
        nameColor = Objects.requireNonNull(nameColor, "nameColor");
        skinId = Objects.requireNonNull(skinId, "skinId");
        activeFormId = Objects.requireNonNull(activeFormId, "activeFormId");
        activeFormRevision = Objects.requireNonNull(activeFormRevision, "activeFormRevision");
        activeFormFingerprint = Objects.requireNonNull(activeFormFingerprint, "activeFormFingerprint");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        status = Objects.requireNonNull(status, "status");
        desiredPresence = Objects.requireNonNull(desiredPresence, "desiredPresence");
        definitionCompatibility = Objects.requireNonNull(definitionCompatibility, "definitionCompatibility");
        materializedPosition = Objects.requireNonNull(materializedPosition, "materializedPosition");
        restAttachment = Objects.requireNonNull(restAttachment, "restAttachment");
        blockedReason = Objects.requireNonNull(blockedReason, "blockedReason");
        if (actorRevision < 0L || repositoryRevision < 0L) {
            throw new IllegalArgumentException("Actor revisions must not be negative.");
        }
    }

    public NpcActorView(
            ActorId actorId,
            NpcDefinitionId definitionId,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId,
            NpcWorldId worldId,
            NpcPosition position,
            NpcRotation rotation,
            Status status,
            long runtimeRevision
    ) {
        this(actorId, definitionId, displayName, nameColor, skinId, Optional.empty(), Optional.empty(),
                Optional.empty(), worldId, position, rotation, status,
                DesiredPresence.ACTIVE, DefinitionCompatibility.COMPATIBLE, Optional.of(position),
                Optional.empty(), runtimeRevision, runtimeRevision, Optional.empty());
    }

    public NpcActorView(
            ActorId actorId,
            NpcDefinitionId definitionId,
            String displayName,
            Optional<NpcSkinId> skinId,
            NpcWorldId worldId,
            NpcPosition position,
            NpcRotation rotation,
            Status status,
            DesiredPresence desiredPresence,
            DefinitionCompatibility definitionCompatibility,
            Optional<NpcPosition> materializedPosition,
            long actorRevision,
            long repositoryRevision,
            Optional<String> blockedReason
    ) {
        this(actorId, definitionId, displayName, Optional.empty(), skinId, Optional.empty(), Optional.empty(),
                Optional.empty(), worldId, position, rotation, status, desiredPresence, definitionCompatibility,
                materializedPosition, Optional.empty(), actorRevision, repositoryRevision, blockedReason);
    }

    public NpcActorView(
            ActorId actorId,
            NpcDefinitionId definitionId,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId,
            NpcWorldId worldId,
            NpcPosition position,
            NpcRotation rotation,
            Status status,
            DesiredPresence desiredPresence,
            DefinitionCompatibility definitionCompatibility,
            Optional<NpcPosition> materializedPosition,
            Optional<NpcRestAttachmentView> restAttachment,
            long actorRevision,
            long repositoryRevision,
            Optional<String> blockedReason
    ) {
        this(actorId, definitionId, displayName, nameColor, skinId, Optional.empty(), Optional.empty(),
                Optional.empty(), worldId, position, rotation, status, desiredPresence, definitionCompatibility,
                materializedPosition, restAttachment, actorRevision, repositoryRevision, blockedReason);
    }

    public NpcActorView(
            ActorId actorId,
            NpcDefinitionId definitionId,
            String displayName,
            Optional<NpcSkinId> skinId,
            NpcWorldId worldId,
            NpcPosition position,
            NpcRotation rotation,
            Status status,
            long runtimeRevision
    ) {
        this(actorId, definitionId, displayName, Optional.empty(), skinId, worldId, position, rotation, status,
                runtimeRevision);
    }

    private static String requireDisplay(String value) {
        String checked = Objects.requireNonNull(value, "displayName");
        if (checked.isBlank() || checked.length() > 64) {
            throw new IllegalArgumentException("displayName must be non-empty and at most 64 characters.");
        }
        return checked;
    }

    public enum Status {
        ACTIVE,
        PLATFORM_MISSING,
        MATERIALIZED,
        NPC_SLEEPING,
        DORMANT_CHUNK_UNLOADED,
        PENDING_SPAWN,
        PENDING_MOVE,
        PENDING_REMOVAL,
        BLOCKED_DEFINITION_MISSING,
        BLOCKED_DEFINITION_INCOMPATIBLE,
        BLOCKED_WORLD_MISSING,
        BLOCKED_CORRUPT_RECORD,
        PLATFORM_FAILURE,
        HOST_STOPPED
    }

    public enum DesiredPresence {
        ACTIVE,
        ABSENT
    }

    public enum DefinitionCompatibility {
        COMPATIBLE,
        MISSING,
        INCOMPATIBLE,
        UNKNOWN
    }
}
