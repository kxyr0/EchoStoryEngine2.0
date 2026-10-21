package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.Objects;

/**
 * Maps durable Story action executions to deterministic Animation command ids.
 */
final class DeterministicAnimationCommandId {
    private static final String COMMAND_NAMESPACE = "echo_story_animation";

    private DeterministicAnimationCommandId() {
    }

    static AnimationCommandId from(ActionExecutionId actionExecutionId, NamespacedId actionType) {
        Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        Objects.requireNonNull(actionType, "actionType");
        String actionPath = actionExecutionId.asString().replace(':', '/');
        return AnimationCommandId.parse(COMMAND_NAMESPACE + ":" + actionType.path() + "/" + actionPath);
    }
}
