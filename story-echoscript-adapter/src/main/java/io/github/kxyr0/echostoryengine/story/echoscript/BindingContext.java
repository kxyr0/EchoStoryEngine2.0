package io.github.kxyr0.echostoryengine.story.echoscript;

import java.time.Instant;

/**
 * Shared source context for one StoryResource binding operation.
 */
record BindingContext(String sourceId, String storyId, String nodeId, Instant occurredAt) {
    static BindingContext source(String sourceId, Instant occurredAt) {
        return new BindingContext(sourceId, "", "", occurredAt);
    }

    BindingContext story(String storyId) {
        return new BindingContext(sourceId, storyId, "", occurredAt);
    }

    BindingContext node(String nodeId) {
        return new BindingContext(sourceId, storyId, nodeId, occurredAt);
    }
}
