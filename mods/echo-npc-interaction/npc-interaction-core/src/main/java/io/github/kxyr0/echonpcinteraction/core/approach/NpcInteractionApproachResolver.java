package io.github.kxyr0.echonpcinteraction.core.approach;

import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NpcInteractionApproachResolver {
    public Optional<NpcInteractionApproach> best(List<NpcInteractionApproach> approaches) {
        return approaches.stream()
                .sorted(Comparator.comparingDouble(NpcInteractionApproach::estimatedApproachCost)
                        .thenComparingDouble(NpcInteractionApproach::requiredReach))
                .findFirst();
    }
}
