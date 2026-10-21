package io.github.kxyr0.echostoryengine.story.forge.resource;

import java.util.Objects;

/**
 * In-memory UTF-8 source из server resources/datapacks.
 */
public record StoryResourceText(String sourceId, String content) {
    public StoryResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        content = Objects.requireNonNull(content, "content");
    }
}
