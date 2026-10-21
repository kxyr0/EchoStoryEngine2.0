package io.github.kxyr0.echoevents.api.context;

import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import java.util.Objects;
import java.util.Optional;

public record EventExecutionContext(ExecutionId root, ExecutionId caller, EventAudience audience,
        Optional<ResourceId> targetResource) {
    public EventExecutionContext {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(targetResource, "targetResource");
    }
    public EventExecutionContext asCaller(ExecutionId execution) {
        return new EventExecutionContext(root, execution, audience, targetResource);
    }
}
