package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.identity.ActorId;

public record WakePayload(ActorId actorId) {
    public WakePayload {
        actorId = java.util.Objects.requireNonNull(actorId, "actorId");
    }
}
