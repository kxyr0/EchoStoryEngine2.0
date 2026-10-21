package io.github.kxyr0.echocutscene.core.lifecycle;

import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import java.util.Objects;

public record CutscenePlayback(
        CutscenePlaybackId playbackId,
        CutsceneOwnerId ownerId,
        CutsceneDefinition definition,
        long startedAtMillis,
        CutsceneStatus status
) {
    public CutscenePlayback {
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        definition = Objects.requireNonNull(definition, "definition");
        status = Objects.requireNonNull(status, "status");
    }

    public CutscenePlayback status(CutsceneStatus next) {
        return new CutscenePlayback(playbackId, ownerId, definition, startedAtMillis, next);
    }

    public CutscenePlayback startedAt(long nextStartedAtMillis) {
        return new CutscenePlayback(playbackId, ownerId, definition, nextStartedAtMillis, status);
    }
}
