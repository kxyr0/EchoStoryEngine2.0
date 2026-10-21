package io.github.kxyr0.echoplayeractor.api.pose;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import java.util.Objects;
import java.util.Optional;

public record PlayerActorSnapshot(
        PlayerActorId actorId,
        PlayerActorTransform transform,
        PlayerActorPose pose,
        PlayerActorSeatSnapshot seat,
        boolean usingItem,
        boolean moving,
        Optional<PlayerActorCommandId> activeCommandId,
        long bodyRevision) {
    public PlayerActorSnapshot {
        actorId = Objects.requireNonNull(actorId, "actorId");
        transform = Objects.requireNonNull(transform, "transform");
        pose = Objects.requireNonNull(pose, "pose");
        seat = Objects.requireNonNull(seat, "seat");
        activeCommandId = Objects.requireNonNull(activeCommandId, "activeCommandId");
        if (bodyRevision < 0L) {
            throw new IllegalArgumentException("bodyRevision must not be negative.");
        }
    }
}
