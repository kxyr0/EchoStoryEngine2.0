package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;

public record DespawnPayload(ActorId actorId) {
    public DespawnPayload {
        actorId = Objects.requireNonNull(actorId, "actorId");
    }
}
