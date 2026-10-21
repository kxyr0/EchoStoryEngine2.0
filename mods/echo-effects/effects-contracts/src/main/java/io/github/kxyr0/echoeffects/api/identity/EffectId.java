package io.github.kxyr0.echoeffects.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public record EffectId(String value) {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public EffectId {
        value = Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("EffectId must be namespace:path.");
        }
    }

    public static EffectId of(String value) {
        return new EffectId(value);
    }
}
