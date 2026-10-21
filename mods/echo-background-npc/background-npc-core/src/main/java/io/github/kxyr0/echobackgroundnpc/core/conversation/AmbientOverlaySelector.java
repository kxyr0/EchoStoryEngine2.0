package io.github.kxyr0.echobackgroundnpc.core.conversation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Selects at most one audible conversation with a stable tie break. */
public final class AmbientOverlaySelector {
    public Optional<Candidate> select(List<Candidate> candidates, double range, boolean storyDialogueVisible) {
        if (storyDialogueVisible) return Optional.empty();
        double rangeSquared = range * range;
        return candidates.stream().filter(Candidate::lineOfSight)
                .filter(value -> value.distanceSquared() <= rangeSquared)
                .min(Comparator.comparingDouble(Candidate::distanceSquared).thenComparing(Candidate::sessionId));
    }

    public record Candidate(String sessionId, double distanceSquared, boolean lineOfSight) { }
}
