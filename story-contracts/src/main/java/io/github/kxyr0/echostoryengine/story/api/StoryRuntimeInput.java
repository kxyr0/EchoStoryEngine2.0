package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
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
    public static final String ACTION_EXECUTION_ID_ATTRIBUTE = "actionExecutionId";
    public static final String ACTION_RESULT_CATEGORY_ATTRIBUTE = "resultCategory";

    private static final NamespacedId START_EVENT = NamespacedId.of("echo_story_engine:runtime_start");
    private static final NamespacedId EXTERNAL_ACTION_COMPLETED_EVENT =
            NamespacedId.of("echo_story_engine:external_action_completed");

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

    /**
     * Creates the explicit input that starts or continues synchronous runtime evaluation.
     */
    public static StoryRuntimeInput start(StoryId storyId, ExecutionId executionId) {
        return new StoryRuntimeInput(storyId, executionId, START_EVENT, Map.of());
    }

    /**
     * Creates explicit input for an external action completion.
     */
    public static StoryRuntimeInput externalActionCompleted(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            ExternalActionResultCategory resultCategory) {
        return new StoryRuntimeInput(
                storyId,
                executionId,
                EXTERNAL_ACTION_COMPLETED_EVENT,
                Map.of(
                        ACTION_EXECUTION_ID_ATTRIBUTE,
                        actionExecutionId.asString(),
                        ACTION_RESULT_CATEGORY_ATTRIBUTE,
                        resultCategory.name()));
    }

    /**
     * Returns whether this input reports external action completion.
     */
    public boolean isExternalActionCompletion() {
        return EXTERNAL_ACTION_COMPLETED_EVENT.equals(eventType);
    }

    /**
     * Returns the completed external action id carried by this input.
     */
    public ActionExecutionId completedActionExecutionId() {
        return ActionExecutionId.of(attributes.get(ACTION_EXECUTION_ID_ATTRIBUTE));
    }

    /**
     * Returns the external action completion category carried by this input.
     */
    public ExternalActionResultCategory completedActionResultCategory() {
        return ExternalActionResultCategory.valueOf(attributes.get(ACTION_RESULT_CATEGORY_ATTRIBUTE));
    }
}
