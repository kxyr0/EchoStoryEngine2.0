package io.github.kxyr0.echobackgroundnpc.core.simulation;

import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.Objects;
import java.util.Optional;

public record AgentTransition(
        BackgroundAgentState previous,
        BackgroundAgentState current,
        String reason,
        Optional<BackgroundSourceRef> source
) {
    public AgentTransition {
        previous = Objects.requireNonNull(previous, "previous");
        current = Objects.requireNonNull(current, "current");
        reason = Objects.requireNonNull(reason, "reason");
        source = Objects.requireNonNull(source, "source");
    }
}
