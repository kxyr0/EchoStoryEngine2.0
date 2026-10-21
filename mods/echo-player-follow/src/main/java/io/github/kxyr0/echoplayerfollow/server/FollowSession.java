package io.github.kxyr0.echoplayerfollow.server;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.UUID;

record FollowSession(UUID playerId, ActorId actorId, int entityId) {
    FollowSession withEntityId(int updatedEntityId) {
        return new FollowSession(playerId, actorId, updatedEntityId);
    }
}
