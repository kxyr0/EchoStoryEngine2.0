package io.github.kxyr0.echocutscene.core.lifecycle;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.persistence.CutscenePlaybackCheckpoint;
import java.util.Objects;

public final class CutscenePlaybackRecovery {
    public CutscenePlayback recover(CutscenePlaybackCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        return new CutscenePlayback(checkpoint.playbackId(), checkpoint.ownerId(), checkpoint.definition(),
                checkpoint.startedAtMillis(), CutsceneStatus.WAITING_FOR_CLIENT);
    }
}
