package io.github.kxyr0.echonpcinteraction.core.provider;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NpcInteractionProviderSelector {
    public NpcInteractionProviderChoice select(List<Candidate> candidates) {
        List<Candidate> supported = candidates.stream()
                .filter(Candidate::supported)
                .sorted(Comparator.comparingInt(Candidate::specificity).reversed())
                .toList();
        if (supported.isEmpty()) {
            return new NpcInteractionProviderChoice(Optional.empty(),
                    Optional.of(NpcInteractionFailure.NPC_INTERACTION_PROVIDER_NOT_FOUND), "Provider not found.");
        }
        Candidate best = supported.get(0);
        boolean conflict = supported.size() > 1 && supported.get(1).specificity() == best.specificity();
        if (conflict) {
            return new NpcInteractionProviderChoice(Optional.empty(),
                    Optional.of(NpcInteractionFailure.NPC_INTERACTION_PROVIDER_CONFLICT), "Provider conflict.");
        }
        return new NpcInteractionProviderChoice(Optional.of(best.providerId()), Optional.empty(), "Provider selected.");
    }

    public record Candidate(NpcInteractionProviderId providerId, int priority, int specificity, boolean supported) {
    }
}
