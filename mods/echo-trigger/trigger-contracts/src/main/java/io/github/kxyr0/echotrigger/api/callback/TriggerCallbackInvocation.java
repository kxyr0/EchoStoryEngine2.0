package io.github.kxyr0.echotrigger.api.callback;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TriggerCallbackInvocation(
        TriggerId triggerId,
        TriggerInstanceId instanceId,
        TriggerOwnerId owner,
        TriggerCallbackKind kind,
        TriggerCallbackSignature signature,
        List<TriggerCallbackAction> actions,
        Map<String, String> payload,
        long serverTick
) {
    public TriggerCallbackInvocation {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        owner = Objects.requireNonNull(owner, "owner");
        kind = Objects.requireNonNull(kind, "kind");
        signature = Objects.requireNonNull(signature, "signature");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        if (serverTick < 0) {
            throw new IllegalArgumentException("serverTick must not be negative.");
        }
    }
}
