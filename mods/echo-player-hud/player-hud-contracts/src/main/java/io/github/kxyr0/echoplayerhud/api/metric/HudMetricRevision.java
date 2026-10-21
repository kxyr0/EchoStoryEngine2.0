package io.github.kxyr0.echoplayerhud.api.metric;

/**
 * Monotonic metric snapshot revision.
 */
public record HudMetricRevision(long value) implements Comparable<HudMetricRevision> {
    public HudMetricRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("HUD metric revision cannot be negative.");
        }
    }

    public static HudMetricRevision unavailable() {
        return new HudMetricRevision(0L);
    }

    @Override
    public int compareTo(HudMetricRevision other) {
        return Long.compare(value, other.value);
    }
}
