package io.github.kxyr0.echoplayer.api.profile;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * EchoScript player profile template identity in namespace:path form.
 */
public record PlayerProfileTemplateId(String namespace, String path) implements Comparable<PlayerProfileTemplateId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public PlayerProfileTemplateId {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
        validate(namespace, path);
    }

    public static PlayerProfileTemplateId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Template ID must be namespace:path.");
        }
        return new PlayerProfileTemplateId(value.substring(0, separator), value.substring(separator + 1));
    }

    public String value() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public int compareTo(PlayerProfileTemplateId other) {
        return value().compareTo(other.value());
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Template ID " + name + " is required.");
        }
        return value;
    }

    private static void validate(String namespace, String path) {
        if (!namespace.equals(namespace.toLowerCase(Locale.ROOT)) || !path.equals(path.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Template ID must be lowercase.");
        }
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Template ID contains invalid resource characters.");
        }
        if (path.contains("..") || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("Template ID path must not contain traversal.");
        }
    }
}
