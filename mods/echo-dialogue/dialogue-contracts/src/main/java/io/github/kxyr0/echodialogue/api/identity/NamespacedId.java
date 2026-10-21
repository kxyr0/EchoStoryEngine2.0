package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable lower-case namespace/path identifier shared by dialogue IDs and speaker IDs.
 */
public record NamespacedId(String value) {
    private static final Pattern PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public NamespacedId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Идентификатор должен иметь вид namespace:path: " + value);
        }
    }

    public String namespace() {
        return value.substring(0, value.indexOf(':'));
    }

    public String path() {
        return value.substring(value.indexOf(':') + 1);
    }
}
