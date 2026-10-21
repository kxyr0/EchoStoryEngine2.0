package io.github.kxyr0.echocutscene.forge.playback.persistence;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.forge.body.PlayerBodySnapshot;
import java.util.UUID;

public record SavedCutscenePlayback(
        UUID playerId,
        CutscenePlaybackId playbackId,
        CutsceneId cutsceneId,
        CutsceneEndBehavior endBehavior,
        CutsceneStatus status,
        long elapsedMillis,
        PlayerBodySnapshot bodySnapshot
) {
}
