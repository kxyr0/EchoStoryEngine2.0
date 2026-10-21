package io.github.kxyr0.echoanimation.api.clip.definition;

import java.util.Objects;

/** One-shot authored event on the shared clip timeline. */
public record AnimationClipEvent(String id, int tick, String payload) {
    public AnimationClipEvent {
        id = Objects.requireNonNull(id, "id");
        payload = Objects.requireNonNull(payload, "payload");
        if (id.isBlank() || tick < 0) {
            throw new IllegalArgumentException("Clip event requires a non-blank id and non-negative tick.");
        }
    }
}
