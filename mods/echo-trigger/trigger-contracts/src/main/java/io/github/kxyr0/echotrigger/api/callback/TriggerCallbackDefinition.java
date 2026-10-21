package io.github.kxyr0.echotrigger.api.callback;

import java.util.List;
import java.util.Objects;

public record TriggerCallbackDefinition(
        TriggerCallbackKind kind,
        String methodName,
        TriggerCallbackSignature signature,
        List<TriggerCallbackAction> actions
) {
    public TriggerCallbackDefinition {
        kind = Objects.requireNonNull(kind, "kind");
        methodName = Objects.requireNonNull(methodName, "methodName").trim();
        signature = Objects.requireNonNull(signature, "signature");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        if (methodName.isEmpty()) {
            throw new IllegalArgumentException("Callback method name must not be blank.");
        }
    }
}
