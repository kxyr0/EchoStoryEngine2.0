package io.github.kxyr0.echostoryengine.story.definition.compile;

import java.util.Objects;

/**
 * In-memory environment source text with caller-supplied source identity.
 */
public record EnvironmentSourceInput(String sourceId, String content) {
    public EnvironmentSourceInput {
        sourceId = requireText(sourceId, "sourceId");
        content = Objects.requireNonNull(content, "content");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
