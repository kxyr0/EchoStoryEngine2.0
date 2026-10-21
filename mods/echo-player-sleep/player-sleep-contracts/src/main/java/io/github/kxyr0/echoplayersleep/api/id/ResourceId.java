package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;
import java.util.regex.Pattern;

public record ResourceId(String value) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public ResourceId {
        value = requireId(value, "resourceId");
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!ID.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be a namespaced id");
        }
        return checked;
    }
}
