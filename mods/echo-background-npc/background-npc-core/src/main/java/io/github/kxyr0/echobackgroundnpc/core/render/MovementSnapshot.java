package io.github.kxyr0.echobackgroundnpc.core.render;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskKind;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import java.util.Objects;

public record MovementSnapshot(
        BackgroundNpcId agentId,
        WorldPoint position,
        double velocityX,
        double velocityY,
        double velocityZ,
        float facing,
        TaskKind task,
        long revision,
        long serverTick
) {
    public MovementSnapshot {
        agentId = Objects.requireNonNull(agentId, "agentId");
        position = Objects.requireNonNull(position, "position");
        task = Objects.requireNonNull(task, "task");
        if (revision < 0L || serverTick < 0L) throw new IllegalArgumentException("Invalid snapshot version.");
    }
}
