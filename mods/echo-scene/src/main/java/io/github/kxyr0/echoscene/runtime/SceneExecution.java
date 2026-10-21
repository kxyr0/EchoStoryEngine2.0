package io.github.kxyr0.echoscene.runtime;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.api.model.SceneInspection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

final class SceneExecution {
    private final SceneDefinition definition;
    private final SceneExecutionId id;
    private final String rootOwner;
    private final Set<SceneStepId> completed = new TreeSet<>();
    private final Set<SceneStepId> pending = new TreeSet<>();
    private final Set<SceneCapability> held = new TreeSet<>();
    private SceneExecutionState state;
    private int stageIndex;
    private String failure;

    SceneExecution(SceneDefinition definition, SceneExecutionId id, String rootOwner) {
        this.definition = definition;
        this.id = id;
        this.rootOwner = rootOwner;
        state = SceneExecutionState.CREATED;
    }

    static SceneExecution recover(SceneDefinition definition, SceneCheckpoint checkpoint) {
        SceneExecution execution = new SceneExecution(definition, checkpoint.executionId(), checkpoint.rootOwner());
        execution.state = checkpoint.state();
        execution.stageIndex = checkpoint.stageIndex();
        execution.completed.addAll(checkpoint.completed());
        execution.failure = checkpoint.failure().orElse(null);
        return execution;
    }

    SceneDefinition definition() { return definition; }
    SceneExecutionId id() { return id; }
    String rootOwner() { return rootOwner; }
    SceneExecutionState state() { return state; }
    void state(SceneExecutionState value) { state = value; }
    int stageIndex() { return stageIndex; }
    void nextStage() { stageIndex++; }
    Set<SceneStepId> completed() { return completed; }
    Set<SceneStepId> pending() { return pending; }
    Set<SceneCapability> held() { return held; }
    void fail(String detail) { failure = detail; state = SceneExecutionState.FAILED; }

    void cleanup(io.github.kxyr0.echoscene.api.command.SceneCommandPort commands,
            io.github.kxyr0.echoscene.api.port.SceneCameraPort camera,
            io.github.kxyr0.echoscene.api.port.SceneLeasePort leases) {
        commands.cancelOwned(id);
        if (held.stream().anyMatch(value -> value.kind() ==
                io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind.CAMERA)) camera.restoreOwned(id);
        leases.releaseOwned(id);
        pending.clear();
        held.clear();
    }

    SceneCheckpoint checkpoint() {
        return new SceneCheckpoint(definition.resourceId(), definition.entry(), definition.revision(), id, rootOwner,
                state, stageIndex, completed, pending, held.stream().toList(), Optional.ofNullable(failure));
    }

    SceneInspection inspection() {
        return new SceneInspection(definition.resourceId(), definition.entry(), id, state, stageIndex,
                pending.stream().toList(), held.stream().toList(), Optional.ofNullable(failure));
    }
}
