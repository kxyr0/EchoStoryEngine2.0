package io.github.kxyr0.echoscene.persistence;

import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SavedScene(SceneDefinition definition, SceneCheckpoint checkpoint, UUID player, long committedTick,
        Optional<String> eventOutbox) {
    public SavedScene(SceneDefinition definition, SceneCheckpoint checkpoint, UUID player, long committedTick) {
        this(definition, checkpoint, player, committedTick, Optional.empty());
    }
    public SavedScene(SceneDefinition definition, SceneCheckpoint checkpoint, UUID player) {
        this(definition, checkpoint, player, 0);
    }
    public SavedScene {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(eventOutbox, "eventOutbox");
        if (committedTick < 0) throw new IllegalArgumentException("Negative Scene commit tick");
        if (!definition.resourceId().equals(checkpoint.resourceId()) || !definition.revision().equals(checkpoint.revision())) {
            throw new IllegalArgumentException("Scene checkpoint definition mismatch");
        }
    }
}
