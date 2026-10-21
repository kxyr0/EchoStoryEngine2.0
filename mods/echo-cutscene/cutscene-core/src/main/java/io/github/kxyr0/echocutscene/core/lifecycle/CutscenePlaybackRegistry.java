package io.github.kxyr0.echocutscene.core.lifecycle;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CutscenePlaybackRegistry {
    private final Map<CutsceneOwnerId, CutscenePlayback> byOwner = new HashMap<>();

    public Optional<CutscenePlayback> active(CutsceneOwnerId ownerId) {
        return Optional.ofNullable(byOwner.get(ownerId));
    }

    public boolean putIfAbsent(CutscenePlayback playback) {
        if (byOwner.containsKey(playback.ownerId())) {
            return false;
        }
        byOwner.put(playback.ownerId(), playback);
        return true;
    }

    public Optional<CutscenePlayback> remove(CutsceneOwnerId ownerId) {
        return Optional.ofNullable(byOwner.remove(ownerId));
    }
}
