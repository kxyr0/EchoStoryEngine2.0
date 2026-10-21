package io.github.kxyr0.echobackgroundnpc.api.runtime;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskSource;
import java.util.Objects;
import java.util.Optional;

/** Compact immutable logical state retained whether or not an Entity exists. */
public record BackgroundAgentState(
        BackgroundNpcId id,
        AgentLifeState life,
        MaterializationState materialization,
        BackgroundSimulationLod lod,
        BackgroundLocationId logicalLocation,
        BackgroundTask task,
        TaskSource owner,
        BackgroundAppearanceId appearance,
        int schedulePhase,
        double routeProgress,
        long revision
) {
    public BackgroundAgentState {
        id = Objects.requireNonNull(id, "id");
        life = Objects.requireNonNull(life, "life");
        materialization = Objects.requireNonNull(materialization, "materialization");
        lod = Objects.requireNonNull(lod, "lod");
        logicalLocation = Objects.requireNonNull(logicalLocation, "logicalLocation");
        task = Objects.requireNonNull(task, "task");
        owner = Objects.requireNonNull(owner, "owner");
        appearance = Objects.requireNonNull(appearance, "appearance");
        if (schedulePhase < -1 || routeProgress < 0.0 || routeProgress > 1.0 || revision < 0L) {
            throw new IllegalArgumentException("Invalid agent state.");
        }
    }

    public Optional<BackgroundLocationId> target() {
        if (task instanceof BackgroundTask.Travel value) return Optional.of(value.destination());
        if (task instanceof BackgroundTask.Stay value) return Optional.of(value.location());
        if (task instanceof BackgroundTask.Perform value) return value.location();
        return ((BackgroundTask.Converse) task).rendezvous();
    }
}
