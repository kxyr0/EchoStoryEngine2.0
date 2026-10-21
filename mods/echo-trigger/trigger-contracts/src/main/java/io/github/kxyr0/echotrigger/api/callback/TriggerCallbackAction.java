package io.github.kxyr0.echotrigger.api.callback;

import java.util.Map;
import java.util.Objects;

public record TriggerCallbackAction(String name, Map<String, String> arguments) {
    public TriggerCallbackAction {
        name = Objects.requireNonNull(name, "name").trim();
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Callback action name must not be blank.");
        }
    }
}
