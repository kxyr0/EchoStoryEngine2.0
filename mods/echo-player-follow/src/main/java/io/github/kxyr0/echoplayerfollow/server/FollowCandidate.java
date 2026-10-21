package io.github.kxyr0.echoplayerfollow.server;

import io.github.kxyr0.echonpc.api.identity.ActorId;

record FollowCandidate(ActorId actorId, String displayName, int entityId, double distance) {
}
