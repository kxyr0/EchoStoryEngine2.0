package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerExpression;
import java.util.Objects;
import java.util.Optional;

public record DialogueTriggerWaitRequest(
        DialogueTriggerSessionKey key,
        TriggerOwnerId owner,
        Optional<TriggerId> resourceTriggerId,
        Optional<TriggerExpression> inlineExpression
) {
    public DialogueTriggerWaitRequest {
        key = Objects.requireNonNull(key, "key");
        owner = Objects.requireNonNull(owner, "owner");
        resourceTriggerId = Objects.requireNonNull(resourceTriggerId, "resourceTriggerId");
        inlineExpression = Objects.requireNonNull(inlineExpression, "inlineExpression");
        if (resourceTriggerId.isEmpty() == inlineExpression.isEmpty()) {
            throw new IllegalArgumentException("Wait request must target exactly one resource or inline expression.");
        }
    }
}
