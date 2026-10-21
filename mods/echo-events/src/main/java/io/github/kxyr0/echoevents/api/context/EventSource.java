package io.github.kxyr0.echoevents.api.context;

import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.identity.EventIds;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record EventSource(ResourceId resource, ExecutionId execution, ExecutionId rootExecution,
        TransitionId transition, EventAudience audience, String module, long gameTick,
        OptionalLong echoTimeMicros, String definitionRevision, Optional<Origin> origin, Optional<ExecutionId> callerExecution) {
    public EventSource(ResourceId resource, ExecutionId execution, ExecutionId rootExecution,
            TransitionId transition, EventAudience audience, String module, long gameTick,
            OptionalLong echoTimeMicros, String definitionRevision, Optional<Origin> origin) {
        this(resource, execution, rootExecution, transition, audience, module, gameTick,
                echoTimeMicros, definitionRevision, origin, origin.flatMap(Origin::caller));
    }
    public EventSource {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(rootExecution, "rootExecution");
        Objects.requireNonNull(transition, "transition");
        Objects.requireNonNull(audience, "audience");
        module = EventIds.text(module);
        if (gameTick < 0) throw new IllegalArgumentException("Negative game tick");
        Objects.requireNonNull(echoTimeMicros, "echoTimeMicros");
        definitionRevision = EventIds.text(definitionRevision);
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(callerExecution, "callerExecution");
    }

    public record Origin(String sourceId, int line, int column, Optional<ExecutionId> caller) {
        public Origin {
            sourceId = EventIds.text(sourceId);
            if (line < 1 || column < 1) throw new IllegalArgumentException("Invalid source location");
            Objects.requireNonNull(caller, "caller");
        }
    }
}
