package io.github.kxyr0.echodebug.core.path;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.Objects;

public record TriggerIdPlan(TriggerId triggerId, String namespace, String path, String className) {
    public TriggerIdPlan {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        namespace = Objects.requireNonNull(namespace, "namespace").trim();
        path = Objects.requireNonNull(path, "path").trim();
        className = Objects.requireNonNull(className, "className").trim();
        if (namespace.isEmpty() || path.isEmpty() || className.isEmpty()) {
            throw new IllegalArgumentException("Trigger id plan requires namespace, path, and class name.");
        }
    }
}
