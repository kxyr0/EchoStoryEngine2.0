package io.github.kxyr0.echostoryengine.story.definition.source;

import java.util.Objects;

/**
 * In-memory story source text with stable caller-supplied identity.
 */
public record StorySourceInput(String sourceId, String content) {
    public StorySourceInput {
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
