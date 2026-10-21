package io.github.kxyr0.echonpcinteraction.api.request;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionExecutionId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTarget;
import java.util.Objects;

public record NpcInteractionRequest(
        NpcInteractionCommandId commandId,
        NpcInteractionExecutionId executionId,
        NpcInteractionOwnerId ownerId,
        ActorId npcId,
        NpcInteractionTarget target,
        String face,
        String hand,
        String approach,
        long createdTick) {
    public NpcInteractionRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        npcId = Objects.requireNonNull(npcId, "npcId");
        target = Objects.requireNonNull(target, "target");
        face = defaultText(face, "AUTO");
        hand = defaultText(hand, "MAIN_HAND");
        approach = defaultText(approach, "AUTO");
        if (createdTick < 0L) {
            throw new IllegalArgumentException("createdTick must not be negative.");
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
