package io.github.kxyr0.echoanimation.api.clip.definition;

import java.util.Objects;
import java.util.Optional;

/**
 * Named tick on the source timeline, optionally scoped to a segment.
 */
public record AnimationClipPoint(String id, int tick, Optional<String> segmentId) {
    public AnimationClipPoint(String id, int tick) {
        this(id, tick, Optional.empty());
    }

    public AnimationClipPoint {
        id = Objects.requireNonNull(id, "id").trim();
        segmentId = Objects.requireNonNull(segmentId, "segmentId").map(String::trim);
        if (id.isEmpty() || !id.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Point id contains invalid characters: " + id);
        }
        if (tick < 0) {
            throw new IllegalArgumentException("Point tick must not be negative.");
        }
    }
}
