package io.github.kxyr0.echocutscene.core.persistence;

import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import java.util.Objects;

public record CutscenePlaybackCheckpoint(
        CutscenePlaybackId playbackId,
        CutsceneOwnerId ownerId,
        CutsceneDefinition definition,
        long startedAtMillis
) {
    public CutscenePlaybackCheckpoint {
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        definition = Objects.requireNonNull(definition, "definition");
    }
}
