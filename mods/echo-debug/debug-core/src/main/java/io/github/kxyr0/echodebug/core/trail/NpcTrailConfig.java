package io.github.kxyr0.echodebug.core.trail;

public record NpcTrailConfig(
        double minimumSampleDistance,
        long maximumSampleIntervalTicks,
        int maxPointsPerTrail,
        int maxTrails
) {
    public static final NpcTrailConfig DEFAULTS = new NpcTrailConfig(0.15D, 10L, 4096, 64);

    public NpcTrailConfig {
        if (minimumSampleDistance < 0.0D || !Double.isFinite(minimumSampleDistance)) {
            throw new IllegalArgumentException("minimumSampleDistance must be finite and non-negative.");
        }
        if (maximumSampleIntervalTicks < 1L) {
            throw new IllegalArgumentException("maximumSampleIntervalTicks must be positive.");
        }
        if (maxPointsPerTrail < 2 || maxTrails < 1) {
            throw new IllegalArgumentException("Trail limits are too small.");
        }
    }
}
