package io.github.kxyr0.echoitem.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable typed item-kind identity.
 */
public record ItemKindId(String namespace, String path) {
    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");
    public static final ItemKindId WEAPON = new ItemKindId("echo_item", "weapon");

    public ItemKindId {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
        if (!namespace.equals(namespace.toLowerCase(Locale.ROOT)) || !path.equals(path.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Item kind ID must be lowercase.");
        }
        if (!PART.matcher(namespace).matches() || !PART.matcher(path).matches()) {
            throw new IllegalArgumentException("Item kind ID contains invalid characters.");
        }
    }

    public static ItemKindId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Item kind ID must be namespace:path.");
        }
        return new ItemKindId(value.substring(0, separator), value.substring(separator + 1));
    }

    public String value() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return value();
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Item kind " + name + " is required.");
        }
        return value;
    }
}
