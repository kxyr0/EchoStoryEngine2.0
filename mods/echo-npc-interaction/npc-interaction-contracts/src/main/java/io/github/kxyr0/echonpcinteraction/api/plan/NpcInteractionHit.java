package io.github.kxyr0.echonpcinteraction.api.plan;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;

public record NpcInteractionHit(Vector3 hitPoint, String blockFace) {
    public NpcInteractionHit {
        hitPoint = Objects.requireNonNull(hitPoint, "hitPoint");
        blockFace = Objects.requireNonNull(blockFace, "blockFace").trim();
        if (blockFace.isEmpty()) {
            throw new IllegalArgumentException("blockFace must be non-empty.");
        }
    }
}
