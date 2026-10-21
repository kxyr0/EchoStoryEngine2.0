package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable local identifier for a story node.
 *
 * <p>Node identity must not be inferred from list position. The initial scaffold uses only `root` and one
 * runtime transition node, but the type exists now because runtime state already needs a stable position.
 */
public record NodeId(String value) {
    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9_.-]*");

    public NodeId {
        value = requireFormat(value);
    }

    /**
     * Creates a node id from a stable local value.
     */
    public static NodeId of(String value) {
        return new NodeId(value);
    }

    /**
     * Returns the initial node id used when an execution first appears.
     */
    public static NodeId root() {
        return new NodeId("root");
    }

    private static String requireFormat(String raw) {
        String checked = Objects.requireNonNull(raw, "value").trim();
        if (!FORMAT.matcher(checked).matches()) {
            throw new IllegalArgumentException("node id has invalid format");
        }
        return checked;
    }
}
