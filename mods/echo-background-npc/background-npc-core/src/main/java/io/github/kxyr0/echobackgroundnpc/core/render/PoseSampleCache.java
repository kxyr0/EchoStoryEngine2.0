package io.github.kxyr0.echobackgroundnpc.core.render;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Shared quantized pose cache; values are immutable and never copied per agent. */
public final class PoseSampleCache<T> {
    private final int capacity;
    private final Map<PoseKey, T> samples = new LinkedHashMap<>(64, 0.75f, true);
    private long hits;
    private long misses;

    public PoseSampleCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive.");
        this.capacity = capacity;
    }

    public synchronized T resolve(PoseKey key, Supplier<T> sampler) {
        T sample = samples.get(key);
        if (sample != null) { hits++; return sample; }
        misses++;
        sample = Objects.requireNonNull(sampler.get(), "sample");
        samples.put(key, sample);
        while (samples.size() > capacity) samples.remove(samples.keySet().iterator().next());
        return sample;
    }

    public synchronized double hitRate() {
        long total = hits + misses;
        return total == 0L ? 0.0 : (double) hits / total;
    }

    public record PoseKey(String rig, long animationRevision, String clip, int quantizedPhase,
            String lod, String boneMask) { }
}
