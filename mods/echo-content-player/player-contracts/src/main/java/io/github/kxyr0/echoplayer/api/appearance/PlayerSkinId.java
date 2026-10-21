package io.github.kxyr0.echoplayer.api.appearance;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Authored Echo skin identity in namespace:path form.
 */
public record PlayerSkinId(String namespace, String path) implements Comparable<PlayerSkinId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public PlayerSkinId {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
        validate(namespace, path);
    }

    public static PlayerSkinId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Skin ID must be namespace:path.");
        }
        return new PlayerSkinId(value.substring(0, separator), value.substring(separator + 1));
    }

    public String value() {
        return namespace + ":" + path;
    }

    public String resourcePath() {
        return "textures/echo_player/skins/" + path + ".png";
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public int compareTo(PlayerSkinId other) {
        return value().compareTo(other.value());
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Skin ID " + name + " is required.");
        }
        return value;
    }

    private static void validate(String namespace, String path) {
        if (!namespace.equals(namespace.toLowerCase(Locale.ROOT)) || !path.equals(path.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Skin ID must be lowercase.");
        }
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Skin ID contains invalid resource characters.");
        }
        if (path.contains("..") || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("Skin ID path must not contain traversal.");
        }
    }
}
