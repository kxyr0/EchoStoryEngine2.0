package io.github.kxyr0.echoitem.api;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable Echo item identity in namespace:path form.
 */
public record EchoItemId(String namespace, String path) implements Comparable<EchoItemId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public EchoItemId {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
        validate(namespace, path);
    }

    public static EchoItemId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Item ID must be namespace:path.");
        }
        return new EchoItemId(value.substring(0, separator), value.substring(separator + 1));
    }

    public static EchoItemId parseForSource(String value, Path sourceFile) {
        EchoItemId id = parse(value);
        id.requireFileStem(sourceFile);
        return id;
    }

    public String value() {
        return namespace + ":" + path;
    }

    public String pathName() {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    public void requireFileStem(Path sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");
        String name = sourceFile.getFileName().toString();
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Source filename must not contain traversal.");
        }
        String suffix = ".weapon.es";
        if (!name.endsWith(suffix)) {
            throw new IllegalArgumentException("Weapon source must end with .weapon.es.");
        }
        String stem = name.substring(0, name.length() - suffix.length());
        if (!pathName().equals(stem)) {
            throw new IllegalArgumentException("Item ID path must match source filename.");
        }
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public int compareTo(EchoItemId other) {
        return value().compareTo(other.value());
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Item ID " + name + " is required.");
        }
        return value;
    }

    private static void validate(String namespace, String path) {
        if (!namespace.equals(namespace.toLowerCase(Locale.ROOT)) || !path.equals(path.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Item ID must be lowercase.");
        }
        if ("minecraft".equals(namespace)) {
            throw new IllegalArgumentException("External Echo items cannot use minecraft namespace.");
        }
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Item ID contains invalid ResourceLocation characters.");
        }
        if (path.contains("..") || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("Item ID path must not contain traversal.");
        }
    }
}
