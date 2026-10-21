package io.github.kxyr0.echoitemattachments.api.model;

import java.util.Objects;
import java.util.Optional;

public record AnimationCue(Optional<String> animationId, String markerId) {
    public AnimationCue {
        animationId = Objects.requireNonNull(animationId, "animation id");
        animationId = animationId.map(String::trim);
        if (animationId.isPresent() && !animationId.orElseThrow()
                .matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("animation id must be namespaced");
        }
        markerId = Objects.requireNonNull(markerId, "marker id").trim();
        if (markerId.isEmpty()) throw new IllegalArgumentException("marker id must be non-empty");
    }

    public static AnimationCue immediate(String markerId) {
        return new AnimationCue(Optional.empty(), markerId);
    }
}
