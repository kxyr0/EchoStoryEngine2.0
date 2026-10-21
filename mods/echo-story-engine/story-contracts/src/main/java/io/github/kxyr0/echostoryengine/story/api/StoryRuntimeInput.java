package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.LinkedHashMap;
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
    public static final String ACTION_OUTPUT_ATTRIBUTE_PREFIX = "output.";

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
        return externalActionCompleted(storyId, executionId,
                new ExternalActionCompletionInput(actionExecutionId, resultCategory, Map.of()));
    }

    /**
     * Creates explicit input for an external action completion with generic output values.
     */
    public static StoryRuntimeInput externalActionCompleted(
            StoryId storyId,
            ExecutionId executionId,
            ExternalActionCompletionInput completion) {
        ExternalActionCompletionInput checked = Objects.requireNonNull(completion, "completion");
        return new StoryRuntimeInput(
                storyId,
                executionId,
                EXTERNAL_ACTION_COMPLETED_EVENT,
                completionAttributes(
                        checked.actionExecutionId(),
                        checked.resultCategory(),
                        checked.outputs()));
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

    /**
     * Returns generic output values carried by the completed external action.
     */
    public Map<String, String> completedActionOutputs() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        attributes.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ACTION_OUTPUT_ATTRIBUTE_PREFIX))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(
                        entry.getKey().substring(ACTION_OUTPUT_ATTRIBUTE_PREFIX.length()),
                        entry.getValue()));
        return Map.copyOf(values);
    }

    private static Map<String, String> completionAttributes(
            ActionExecutionId actionExecutionId,
            ExternalActionResultCategory resultCategory,
            Map<String, String> outputs) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(ACTION_EXECUTION_ID_ATTRIBUTE, actionExecutionId.asString());
        values.put(ACTION_RESULT_CATEGORY_ATTRIBUTE, resultCategory.name());
        Objects.requireNonNull(outputs, "outputs").entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(ACTION_OUTPUT_ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue()));
        return Map.copyOf(values);
    }

    /**
     * Neutral data carried by one completed external action.
     */
    public record ExternalActionCompletionInput(
            ActionExecutionId actionExecutionId,
            ExternalActionResultCategory resultCategory,
            Map<String, String> outputs) {
        public ExternalActionCompletionInput {
            actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
            resultCategory = Objects.requireNonNull(resultCategory, "resultCategory");
            outputs = Map.copyOf(Objects.requireNonNull(outputs, "outputs"));
        }
    }
}
