package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import java.util.Objects;

public record DialogueTriggerWaitRegistration(
        DialogueTriggerSessionKey key,
        TriggerInstanceId instanceId,
        boolean inline
) {
    public DialogueTriggerWaitRegistration {
        key = Objects.requireNonNull(key, "key");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
    }
}
