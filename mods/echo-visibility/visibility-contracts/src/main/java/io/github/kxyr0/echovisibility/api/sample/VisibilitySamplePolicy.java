package io.github.kxyr0.echovisibility.api.sample;

import java.util.List;
import java.util.Objects;

public record VisibilitySamplePolicy(List<VisibilitySampleKind> kinds, int maximumSamples) {
    public VisibilitySamplePolicy {
        kinds = List.copyOf(Objects.requireNonNull(kinds, "kinds"));
        if (kinds.isEmpty() || kinds.stream().distinct().count() != kinds.size()) {
            throw new IllegalArgumentException("Visibility sample kinds must be non-empty and unique.");
        }
        if (maximumSamples < 1 || maximumSamples > 64) {
            throw new IllegalArgumentException("Maximum visibility samples must be within 1..64.");
        }
    }

    public static VisibilitySamplePolicy npcDefaults() {
        return new VisibilitySamplePolicy(List.of(VisibilitySampleKind.NPC_HEAD,
                VisibilitySampleKind.NPC_CHEST, VisibilitySampleKind.NPC_PELVIS,
                VisibilitySampleKind.NPC_FEET, VisibilitySampleKind.BOUNDS_CENTER,
                VisibilitySampleKind.BOUNDS_CORNERS), 27);
    }

    public static VisibilitySamplePolicy boxDefaults() {
        return new VisibilitySamplePolicy(List.of(VisibilitySampleKind.BOX_CENTER,
                VisibilitySampleKind.BOX_CORNERS, VisibilitySampleKind.BOX_FACE_CENTERS), 27);
    }
}
