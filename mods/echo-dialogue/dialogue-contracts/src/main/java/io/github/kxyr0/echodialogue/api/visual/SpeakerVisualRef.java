package io.github.kxyr0.echodialogue.api.visual;

import java.util.Objects;

/**
 * Immutable neutral reference to one optional speaker visual.
 */
public record SpeakerVisualRef(SpeakerVisualKind kind, SpeakerVisualId id) {
    public SpeakerVisualRef {
        kind = Objects.requireNonNull(kind, "kind");
        id = Objects.requireNonNull(id, "id");
    }

    public static SpeakerVisualRef portrait(String rawId) {
        return new SpeakerVisualRef(SpeakerVisualKind.PORTRAIT, SpeakerVisualId.parse(rawId));
    }

    public static SpeakerVisualRef skinHead(String rawId) {
        return new SpeakerVisualRef(SpeakerVisualKind.SKIN_HEAD, SpeakerVisualId.parse(rawId));
    }
}
