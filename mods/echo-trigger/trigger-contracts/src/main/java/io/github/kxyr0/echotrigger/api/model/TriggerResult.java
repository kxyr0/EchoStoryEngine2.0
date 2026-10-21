package io.github.kxyr0.echotrigger.api.model;

import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import java.util.Objects;
import java.util.Optional;

public record TriggerResult(
        TriggerResultCode code,
        Optional<TriggerInstanceId> instanceId,
        Optional<TriggerSnapshot> snapshot,
        Optional<String> message
) {
    public TriggerResult {
        code = Objects.requireNonNull(code, "code");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        message = Objects.requireNonNull(message, "message");
    }

    public static TriggerResult of(TriggerResultCode code, TriggerSnapshot snapshot) {
        return new TriggerResult(code, Optional.of(snapshot.instanceId()), Optional.of(snapshot), Optional.empty());
    }

    public static TriggerResult rejected(TriggerResultCode code, String message) {
        return new TriggerResult(code, Optional.empty(), Optional.empty(), Optional.of(message));
    }
}
