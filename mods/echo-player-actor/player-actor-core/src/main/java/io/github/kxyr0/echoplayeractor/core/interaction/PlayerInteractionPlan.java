package io.github.kxyr0.echoplayeractor.core.interaction;

import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import java.util.Objects;

public record PlayerInteractionPlan(
        PlayerActorInteractionRequest request,
        double distance,
        boolean withinReach,
        boolean lineOfSight,
        String receiptKey) {
    public PlayerInteractionPlan {
        request = Objects.requireNonNull(request, "request");
        receiptKey = Objects.requireNonNull(receiptKey, "receiptKey");
    }
}
