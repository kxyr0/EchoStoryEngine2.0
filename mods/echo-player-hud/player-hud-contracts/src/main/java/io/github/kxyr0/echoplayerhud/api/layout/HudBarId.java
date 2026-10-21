package io.github.kxyr0.echoplayerhud.api.layout;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable layout element identifier.
 */
public record HudBarId(String value) implements Comparable<HudBarId> {
    private static final Pattern ALLOWED = Pattern.compile("[a-z0-9_.:-]{1,96}");

    public HudBarId {
        value = Objects.requireNonNull(value, "value").strip();
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("HUD bar ID must be 1-96 lowercase safe identifier characters.");
        }
    }

    @Override
    public int compareTo(HudBarId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
