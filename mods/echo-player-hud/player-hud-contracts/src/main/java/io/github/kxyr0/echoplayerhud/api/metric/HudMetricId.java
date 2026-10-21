package io.github.kxyr0.echoplayerhud.api.metric;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable metric identifier used by providers and bars.
 */
public record HudMetricId(String value) implements Comparable<HudMetricId> {
    private static final Pattern ALLOWED = Pattern.compile("[a-z0-9_.:-]{1,96}");

    public HudMetricId {
        value = Objects.requireNonNull(value, "value").strip();
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("HUD metric ID must be 1-96 lowercase safe identifier characters.");
        }
    }

    @Override
    public int compareTo(HudMetricId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
