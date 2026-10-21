package io.github.kxyr0.echonpc.persistence.schema;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;

/**
 * Maps stable actor desired state to Actor Save Schema V1.
 */
public final class ActorSaveMapper {
    public ActorSaveRecord toRecord(ActorDesiredState state) {
        return new ActorSaveRecord(4, state.actorId().value(), state.definitionId().value(),
                state.definitionFingerprint().value(), state.presence().name(), state.worldId().value(),
                state.position().x(), state.position().y(), state.position().z(), state.rotation().yaw(),
                state.rotation().pitch(), state.entityBindingId().value(), state.actorRevision(),
                state.repositoryRevision(), state.createdAtEpochMillis(), state.updatedAtEpochMillis(),
                state.activeSkinId().map(NpcSkinId::value), activeFormRecord(state),
                state.restAttachment().map(this::restRecord));
    }

    public ActorDesiredState toState(ActorSaveRecord record) {
        ActorId actorId = new ActorId(record.actorId());
        return new ActorDesiredState(actorId, new NpcDefinitionId(record.definitionId()),
                new NpcDefinitionFingerprint(record.definitionFingerprint()), presence(record.presence()),
                new NpcWorldId(record.worldId()), new NpcPosition(record.x(), record.y(), record.z()),
                new NpcRotation(record.yaw(), record.pitch()),
                record.activeSkinId().map(NpcSkinId::new),
                record.activeForm().map(form -> new NpcFormId(form.formId())),
                record.activeForm().map(form -> new NpcFormRevision(form.revision())),
                record.activeForm().map(FormSaveRecord::fingerprint), record.restAttachment()
                        .map(rest -> restAttachment(rest, actorId)),
                new EntityBindingId(record.entityBindingId()), record.actorRevision(), record.repositoryRevision(),
                record.createdAtEpochMillis(), record.updatedAtEpochMillis());
    }

    private java.util.Optional<FormSaveRecord> activeFormRecord(ActorDesiredState state) {
        if (state.activeFormId().isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new FormSaveRecord(state.activeFormId().orElseThrow().value(),
                state.activeFormRevision().orElseThrow().value(), state.activeFormFingerprint().orElseThrow()));
    }

    private RestSaveRecord restRecord(NpcRestAttachment rest) {
        return new RestSaveRecord(rest.providerId(), rest.worldId().value(), rest.bedPosition().x(),
                rest.bedPosition().y(), rest.bedPosition().z(), rest.expectedBlock().value(),
                rest.canonicalBedPosition().x(), rest.canonicalBedPosition().y(), rest.canonicalBedPosition().z(),
                rest.sleepAnchor().x(), rest.sleepAnchor().y(), rest.sleepAnchor().z(), rest.sleepYaw(),
                rest.pose(), rest.attachmentRevision(), rest.providerState(), rest.occupied(),
                rest.commandId().value());
    }

    private NpcRestAttachment restAttachment(RestSaveRecord rest, ActorId actorId) {
        return new NpcRestAttachment(actorId, rest.providerId(), new NpcWorldId(rest.bedWorldId()),
                new BlockPosition(rest.bedX(), rest.bedY(), rest.bedZ()), new BlockId(rest.expectedBlockId()),
                new BlockPosition(rest.canonicalBedX(), rest.canonicalBedY(), rest.canonicalBedZ()),
                new NpcPosition(rest.anchorX(), rest.anchorY(), rest.anchorZ()), rest.sleepYaw(), rest.pose(),
                rest.attachmentRevision(), rest.providerState(), rest.occupied(),
                new NpcSceneCommandId(rest.commandId()));
    }

    private ActorPresence presence(String value) {
        try {
            return ActorPresence.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown actor presence: " + value);
        }
    }
}
