package io.github.kxyr0.echoplayeractor.api.request;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;
import java.util.Optional;

public record PlayerActorInteractionRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        int x,
        int y,
        int z,
        Optional<String> face,
        String hand,
        double maxDistance) {
    public PlayerActorInteractionRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        face = Objects.requireNonNull(face, "face");
        hand = Objects.requireNonNull(hand, "hand").trim();
        if (hand.isEmpty()) {
            throw new IllegalArgumentException("hand must not be blank.");
        }
        if (!Double.isFinite(maxDistance) || maxDistance <= 0.0D) {
            throw new IllegalArgumentException("maxDistance must be positive and finite.");
        }
    }
}
