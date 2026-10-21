package io.github.kxyr0.echonavigation.api.movement;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;

public record NavigationProgressSnapshot(
        Vector3 previousPosition,
        Vector3 currentPosition,
        double remainingDistance,
        double bestDistance,
        long lastProgressTick,
        long serverTick,
        int pathIndex,
        int repathCount,
        long movementTicks) {
    public NavigationProgressSnapshot {
        previousPosition = Objects.requireNonNull(previousPosition, "previousPosition");
        currentPosition = Objects.requireNonNull(currentPosition, "currentPosition");
        if (remainingDistance < 0.0D || bestDistance < 0.0D) {
            throw new IllegalArgumentException("Distances must not be negative.");
        }
        if (lastProgressTick < 0L || serverTick < 0L || pathIndex < 0 || repathCount < 0 || movementTicks < 0L) {
            throw new IllegalArgumentException("Progress counters must not be negative.");
        }
    }

    public static NavigationProgressSnapshot initial(Vector3 position, double distance, long tick) {
        return new NavigationProgressSnapshot(position, position, distance, distance, tick, tick, 0, 0, 0L);
    }
}
