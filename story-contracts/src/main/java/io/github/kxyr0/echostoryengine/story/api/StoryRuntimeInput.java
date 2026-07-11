package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Map;
import java.util.Objects;

/**
 * Neutral input delivered to a story execution.
 *
 * <p>The event type is namespaced by Story Engine contracts rather than Minecraft APIs. Attributes are bounded
 * key/value facts supplied by an adapter; callers must not place platform objects or serialized state dumps here.
 */
public record StoryRuntimeInput(
        StoryId storyId,
        ExecutionId executionId,
        NamespacedId eventType,
        Map<String, String> attributes) {
    public StoryRuntimeInput {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        eventType = Objects.requireNonNull(eventType, "eventType");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    /**
     * Creates input with defensive copies of all boundary data.
     */
    public static StoryRuntimeInput of(
            StoryId storyId,
            ExecutionId executionId,
            NamespacedId eventType,
            Map<String, String> attributes) {
        return new StoryRuntimeInput(storyId, executionId, eventType, attributes);
    }
}
