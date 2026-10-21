package io.github.kxyr0.echoplayeractor.api.request;

import java.util.Objects;
import java.util.Optional;

public record PlayerActorInteractionResult(
        boolean accepted,
        String targetBlock,
        String hand,
        Optional<String> face,
        double distance,
        boolean lineOfSight,
        Optional<String> receipt,
        String message) {
    public PlayerActorInteractionResult {
        targetBlock = Objects.requireNonNull(targetBlock, "targetBlock");
        hand = Objects.requireNonNull(hand, "hand");
        face = Objects.requireNonNull(face, "face");
        receipt = Objects.requireNonNull(receipt, "receipt");
        message = Objects.requireNonNull(message, "message");
    }
}
