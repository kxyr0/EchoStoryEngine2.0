package io.github.kxyr0.echocutscene.core.lifecycle;

import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import java.util.Objects;

public final class CutscenePlaybackStarter {
    public CutscenePlayback start(CutsceneOwnerId ownerId, CutsceneDefinition definition, long nowMillis) {
        return new CutscenePlayback(CutscenePlaybackId.create(), Objects.requireNonNull(ownerId, "ownerId"),
                Objects.requireNonNull(definition, "definition"), nowMillis, CutsceneStatus.STARTING);
    }
}
