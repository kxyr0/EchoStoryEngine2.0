package io.github.kxyr0.echoitemattachments.core.motion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class HandVelocityWindow {
    private static final int MAX_SAMPLES = 4;
    private final Deque<Sample> samples = new ArrayDeque<>();

    public synchronized void sample(long serverTick, Vector3d position) {
        if (!samples.isEmpty() && serverTick <= samples.getLast().serverTick()) return;
        samples.addLast(new Sample(serverTick, position));
        while (samples.size() > MAX_SAMPLES) samples.removeFirst();
    }

    public synchronized Optional<Vector3d> velocity(double maximumBlocksPerTick) {
        if (samples.size() < 2) return Optional.empty();
        Sample first = samples.getFirst();
        Sample last = samples.getLast();
        long ticks = last.serverTick() - first.serverTick();
        if (ticks <= 0 || ticks > 6) return Optional.empty();
        return Optional.of(last.position().subtract(first.position())
                .scale(1.0D / ticks).clamp(maximumBlocksPerTick));
    }

    public synchronized void clear() {
        samples.clear();
    }

    private record Sample(long serverTick, Vector3d position) {
    }
}
