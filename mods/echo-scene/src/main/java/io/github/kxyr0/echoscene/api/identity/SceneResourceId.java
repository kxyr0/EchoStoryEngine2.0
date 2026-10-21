package io.github.kxyr0.echoscene.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public record SceneResourceId(String value) implements Comparable<SceneResourceId> {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public SceneResourceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid scene resource id: " + value);
        }
    }

    @Override
    public int compareTo(SceneResourceId other) {
        return value.compareTo(other.value);
    }
}
