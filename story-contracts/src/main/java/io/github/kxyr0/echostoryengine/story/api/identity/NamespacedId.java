package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Story Engine namespaced identifier.
 *
 * <p>This type deliberately does not wrap Minecraft {@code ResourceLocation}. It keeps public and core contracts
 * portable across loaders and game versions while preserving familiar namespace/path semantics.
 */
public record NamespacedId(String namespace, String path) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z][a-z0-9_.-]*");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public NamespacedId {
        namespace = requireMatch(namespace, "namespace", NAMESPACE);
        path = requireMatch(path, "path", PATH);
    }

    /**
     * Parses an identifier in `namespace:path` form.
     */
    public static NamespacedId of(String raw) {
        String checked = Objects.requireNonNull(raw, "raw").trim();
        int separator = checked.indexOf(':');
        if (separator <= 0 || separator == checked.length() - 1) {
            throw new IllegalArgumentException("namespaced id must use namespace:path");
        }
        return new NamespacedId(checked.substring(0, separator), checked.substring(separator + 1));
    }

    /**
     * Returns the canonical `namespace:path` representation.
     */
    public String asString() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return asString();
    }

    private static String requireMatch(String value, String name, Pattern pattern) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!pattern.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " has invalid format");
        }
        return checked;
    }
}
