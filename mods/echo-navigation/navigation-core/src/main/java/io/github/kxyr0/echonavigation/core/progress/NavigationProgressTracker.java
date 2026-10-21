package io.github.kxyr0.echonavigation.core.progress;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.position.Vector3;

public final class NavigationProgressTracker {
    public Sample sample(
            NavigationProgressSnapshot previous,
            Vector3 current,
            double remainingDistance,
            int pathIndex,
            long tick,
            NavigationSettingsSnapshot settings) {
        if (tick - previous.lastProgressTick() < settings.progressSampleIntervalTicks()) {
            return new Sample(previous, ProgressDecision.TOO_EARLY);
        }
        double best = Math.min(previous.bestDistance(), remainingDistance);
        boolean advanced = pathIndex > previous.pathIndex();
        boolean improved = advanced || previous.bestDistance() - best >= settings.minimumProgressDistance();
        long progressTick = improved ? tick : previous.lastProgressTick();
        NavigationProgressSnapshot next = new NavigationProgressSnapshot(
                previous.currentPosition(), current, remainingDistance, best, progressTick, tick, pathIndex,
                previous.repathCount(), previous.movementTicks() + 1L);
        return new Sample(next, improved ? ProgressDecision.IMPROVED : ProgressDecision.STALLED);
    }

    public record Sample(NavigationProgressSnapshot progress, ProgressDecision decision) {
    }
}
