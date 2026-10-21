package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import java.util.Objects;
import java.util.Optional;

record InteractionRecord(
        NpcInteractionRequest request,
        NpcInteractionStatus status,
        NpcInteractionPlan plan,
        Optional<NavigationMoveId> moveId,
        long startedTick) {
    InteractionRecord {
        request = Objects.requireNonNull(request, "request");
        status = Objects.requireNonNull(status, "status");
        plan = Objects.requireNonNull(plan, "plan");
        moveId = Objects.requireNonNull(moveId, "moveId");
    }

    InteractionRecord withStatus(NpcInteractionStatus next) {
        return new InteractionRecord(request, next, plan, moveId, startedTick);
    }

    InteractionRecord withMove(NavigationMoveId nextMove) {
        return new InteractionRecord(request, status, plan, Optional.of(nextMove), startedTick);
    }
}
