package io.github.kxyr0.echonpcinteraction.core.validate;

import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import java.util.Optional;
import java.util.Set;

public final class NpcInteractionPlanValidator {
    private static final Set<String> POLICIES = Set.of(
            "BLOCK_STATE_CHANGED", "PROVIDER_CONFIRMED", "NPC_SEATED", "INTERACTION_ACCEPTED");

    public Optional<NpcInteractionFailure> validate(NpcInteractionPlan plan) {
        if (plan.approaches().isEmpty()) {
            return Optional.of(NpcInteractionFailure.NPC_INTERACTION_NO_APPROACH);
        }
        if (!POLICIES.contains(plan.verificationPolicy())) {
            return Optional.of(NpcInteractionFailure.NPC_INTERACTION_PLAN_INVALID);
        }
        return Optional.empty();
    }
}
