package io.github.kxyr0.echoplayeractor.api.request.hotbar;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerHotbarItemRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        int slot,
        String itemId,
        int count,
        boolean replace) {
    public PlayerHotbarItemRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        itemId = Objects.requireNonNull(itemId, "itemId").trim();
        if (slot < 1 || slot > 9) {
            throw new IllegalArgumentException("slot must be authored hotbar slot 1..9.");
        }
        if (itemId.isEmpty() || !itemId.contains(":")) {
            throw new IllegalArgumentException("itemId must be namespaced.");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive.");
        }
    }
}
