package io.github.kxyr0.echobackgroundnpc.core.conversation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Bounds nearby ambient decoding while foreground story voice always suppresses it. */
public final class AmbientVoiceBudget {
    private final int maximumVoices;
    private final double rangeSquared;

    public AmbientVoiceBudget(int maximumVoices, double range) {
        if (maximumVoices < 0 || range <= 0.0) throw new IllegalArgumentException("Invalid voice budget.");
        this.maximumVoices = maximumVoices;
        rangeSquared = range * range;
    }

    public Set<String> eligible(List<VoiceCandidate> candidates, boolean foregroundVoice) {
        if (foregroundVoice) return Set.of();
        return candidates.stream().filter(value -> value.distanceSquared() <= rangeSquared)
                .sorted(Comparator.comparingDouble(VoiceCandidate::distanceSquared)
                        .thenComparing(VoiceCandidate::sessionId))
                .limit(maximumVoices).map(VoiceCandidate::sessionId).collect(Collectors.toUnmodifiableSet());
    }

    public record VoiceCandidate(String sessionId, double distanceSquared) { }
}
