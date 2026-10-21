package io.github.kxyr0.echonpc.core.actor;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.util.Objects;
import java.util.Optional;

/**
 * Durable logical actor checkpoint. Platform materialization is intentionally absent.
 */
public record ActorDesiredState(
        ActorId actorId,
        NpcDefinitionId definitionId,
        NpcDefinitionFingerprint definitionFingerprint,
        ActorPresence presence,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation,
        Optional<NpcSkinId> activeSkinId,
        Optional<NpcFormId> activeFormId,
        Optional<NpcFormRevision> activeFormRevision,
        Optional<String> activeFormFingerprint,
        Optional<NpcRestAttachment> restAttachment,
        EntityBindingId entityBindingId,
        long actorRevision,
        long repositoryRevision,
        long createdAtEpochMillis,
        long updatedAtEpochMillis
) {
    public ActorDesiredState {
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        definitionFingerprint = Objects.requireNonNull(definitionFingerprint, "definitionFingerprint");
        presence = Objects.requireNonNull(presence, "presence");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        activeSkinId = Objects.requireNonNull(activeSkinId, "activeSkinId");
        activeFormId = Objects.requireNonNull(activeFormId, "activeFormId");
        activeFormRevision = Objects.requireNonNull(activeFormRevision, "activeFormRevision");
        activeFormFingerprint = Objects.requireNonNull(activeFormFingerprint, "activeFormFingerprint");
        restAttachment = Objects.requireNonNull(restAttachment, "restAttachment");
        entityBindingId = Objects.requireNonNull(entityBindingId, "entityBindingId");
        if (actorRevision < 0L || repositoryRevision < 0L) {
            throw new IllegalArgumentException("Actor revisions must not be negative.");
        }
        if (createdAtEpochMillis < 0L || updatedAtEpochMillis < 0L) {
            throw new IllegalArgumentException("Actor timestamps must not be negative.");
        }
        activeFormFingerprint.ifPresent(ActorDesiredState::requireFingerprint);
    }

    public ActorDesiredState(ActorId actorId, NpcDefinitionId definitionId,
            NpcDefinitionFingerprint definitionFingerprint, ActorPresence presence, NpcWorldId worldId,
            NpcPosition position, NpcRotation rotation, EntityBindingId entityBindingId, long actorRevision,
            long repositoryRevision, long createdAtEpochMillis, long updatedAtEpochMillis) {
        this(actorId, definitionId, definitionFingerprint, presence, worldId, position, rotation, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), entityBindingId, actorRevision,
                repositoryRevision, createdAtEpochMillis, updatedAtEpochMillis);
    }

    public ActorDesiredState(ActorId actorId, NpcDefinitionId definitionId,
            NpcDefinitionFingerprint definitionFingerprint, ActorPresence presence, NpcWorldId worldId,
            NpcPosition position, NpcRotation rotation, Optional<NpcFormId> activeFormId,
            Optional<NpcFormRevision> activeFormRevision, Optional<String> activeFormFingerprint,
            Optional<NpcRestAttachment> restAttachment, EntityBindingId entityBindingId, long actorRevision,
            long repositoryRevision, long createdAtEpochMillis, long updatedAtEpochMillis) {
        this(actorId, definitionId, definitionFingerprint, presence, worldId, position, rotation, Optional.empty(),
                activeFormId, activeFormRevision, activeFormFingerprint, restAttachment, entityBindingId,
                actorRevision, repositoryRevision, createdAtEpochMillis, updatedAtEpochMillis);
    }

    public ActorDesiredState withRepositoryRevision(long revision) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, position,
                rotation, activeSkinId, activeFormId, activeFormRevision, activeFormFingerprint, restAttachment,
                entityBindingId, actorRevision, revision, createdAtEpochMillis, updatedAtEpochMillis);
    }

    public ActorDesiredState withTarget(NpcWorldId targetWorld, NpcPosition targetPosition,
            NpcRotation targetRotation, long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, targetWorld,
                targetPosition, targetRotation, activeSkinId, activeFormId, activeFormRevision, activeFormFingerprint,
                restAttachment, entityBindingId, actorRevision + 1L, repositoryRevision, createdAtEpochMillis, now);
    }

    public ActorDesiredState absent(long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, ActorPresence.ABSENT, worldId,
                position, rotation, activeSkinId, activeFormId, activeFormRevision, activeFormFingerprint,
                Optional.empty(), entityBindingId, actorRevision + 1L, repositoryRevision, createdAtEpochMillis, now);
    }

    public ActorDesiredState withBinding(EntityBindingId bindingId, long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, position,
                rotation, activeSkinId, activeFormId, activeFormRevision, activeFormFingerprint, restAttachment,
                bindingId,
                actorRevision + 1L, repositoryRevision, createdAtEpochMillis, now);
    }

    public ActorDesiredState withSkin(NpcSkinId skinId, long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, position,
                rotation, Optional.of(Objects.requireNonNull(skinId, "skinId")), activeFormId,
                activeFormRevision, activeFormFingerprint, restAttachment, entityBindingId, actorRevision + 1L,
                repositoryRevision, createdAtEpochMillis, now);
    }

    public ActorDesiredState withForm(NpcFormDefinition form, long now) {
        return withForm(form.id(), form.revision(), form.fingerprint(), now);
    }

    public ActorDesiredState withInitialForm(NpcFormDefinition form) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, position,
                rotation, activeSkinId, Optional.of(form.id()), Optional.of(form.revision()),
                Optional.of(form.fingerprint()), restAttachment, entityBindingId, actorRevision,
                repositoryRevision, createdAtEpochMillis, updatedAtEpochMillis);
    }

    public ActorDesiredState withForm(NpcFormId formId, NpcFormRevision revision, String fingerprint, long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, position,
                rotation, activeSkinId, Optional.of(formId), Optional.of(revision), Optional.of(fingerprint),
                restAttachment, entityBindingId, actorRevision + 1L, repositoryRevision, createdAtEpochMillis, now);
    }

    public ActorDesiredState withRestAttachment(NpcRestAttachment attachment, long now) {
        NpcRotation sleepRotation = new NpcRotation(attachment.sleepYaw(), 0.0F);
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, attachment.worldId(),
                attachment.sleepAnchor(), sleepRotation, activeSkinId, activeFormId, activeFormRevision,
                activeFormFingerprint, Optional.of(attachment), entityBindingId, actorRevision + 1L, repositoryRevision,
                createdAtEpochMillis, now);
    }

    public ActorDesiredState withoutRestAttachment(NpcPosition wakePosition, NpcRotation wakeRotation, long now) {
        return new ActorDesiredState(actorId, definitionId, definitionFingerprint, presence, worldId, wakePosition,
                wakeRotation, activeSkinId, activeFormId, activeFormRevision, activeFormFingerprint,
                Optional.empty(), entityBindingId, actorRevision + 1L, repositoryRevision, createdAtEpochMillis, now);
    }

    private static void requireFingerprint(String value) {
        if (!value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("activeFormFingerprint must be SHA-256 hex.");
        }
    }
}
