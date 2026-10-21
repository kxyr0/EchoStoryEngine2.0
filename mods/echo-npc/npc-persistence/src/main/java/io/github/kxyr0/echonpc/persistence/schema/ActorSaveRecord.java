package io.github.kxyr0.echonpc.persistence.schema;

import java.util.Objects;
import java.util.Optional;

/**
 * Echo NPC Actor Save Schema DTO.
 */
public record ActorSaveRecord(
        int schemaVersion,
        String actorId,
        String definitionId,
        String definitionFingerprint,
        String presence,
        String worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String entityBindingId,
        long actorRevision,
        long repositoryRevision,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        Optional<String> activeSkinId,
        Optional<FormSaveRecord> activeForm,
        Optional<RestSaveRecord> restAttachment
) {
    public ActorSaveRecord {
        require(schemaVersion >= 1 && schemaVersion <= 4, "schemaVersion must be between 1 and 4.");
        actorId = text(actorId, "actorId");
        definitionId = text(definitionId, "definitionId");
        definitionFingerprint = text(definitionFingerprint, "definitionFingerprint");
        presence = text(presence, "presence");
        worldId = text(worldId, "worldId");
        entityBindingId = text(entityBindingId, "entityBindingId");
        require(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z), "position must be finite.");
        require(Float.isFinite(yaw) && Float.isFinite(pitch), "rotation must be finite.");
        require(actorRevision >= 0L && repositoryRevision >= 0L, "revisions must not be negative.");
        require(createdAtEpochMillis >= 0L && updatedAtEpochMillis >= 0L, "timestamps must not be negative.");
        activeSkinId = Objects.requireNonNull(activeSkinId, "activeSkinId").map(value -> text(value, "activeSkinId"));
        activeForm = Objects.requireNonNull(activeForm, "activeForm");
        restAttachment = Objects.requireNonNull(restAttachment, "restAttachment");
    }

    public ActorSaveRecord(int schemaVersion, String actorId, String definitionId,
            String definitionFingerprint, String presence, String worldId, double x, double y, double z,
            float yaw, float pitch, String entityBindingId, long actorRevision, long repositoryRevision,
            long createdAtEpochMillis, long updatedAtEpochMillis) {
        this(schemaVersion, actorId, definitionId, definitionFingerprint, presence, worldId, x, y, z, yaw, pitch,
                entityBindingId, actorRevision, repositoryRevision, createdAtEpochMillis, updatedAtEpochMillis,
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    public ActorSaveRecord(int schemaVersion, String actorId, String definitionId,
            String definitionFingerprint, String presence, String worldId, double x, double y, double z,
            float yaw, float pitch, String entityBindingId, long actorRevision, long repositoryRevision,
            long createdAtEpochMillis, long updatedAtEpochMillis, Optional<RestSaveRecord> restAttachment) {
        this(schemaVersion, actorId, definitionId, definitionFingerprint, presence, worldId, x, y, z, yaw, pitch,
                entityBindingId, actorRevision, repositoryRevision, createdAtEpochMillis, updatedAtEpochMillis,
                Optional.empty(), Optional.empty(), restAttachment);
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 256) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
