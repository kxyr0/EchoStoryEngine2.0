package io.github.kxyr0.echoplayeractor.api.request.hotbar;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerHotbarSelectionRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        int slot) {
    public PlayerHotbarSelectionRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        if (slot < 1 || slot > 9) {
            throw new IllegalArgumentException("slot must be authored hotbar slot 1..9.");
        }
    }
}
