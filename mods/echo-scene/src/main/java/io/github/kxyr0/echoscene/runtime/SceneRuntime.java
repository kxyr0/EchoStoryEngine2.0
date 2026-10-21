package io.github.kxyr0.echoscene.runtime;

import io.github.kxyr0.echoscene.api.command.SceneCommandPort;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.definition.SceneStage;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.api.model.SceneInspection;
import io.github.kxyr0.echoscene.api.port.SceneCameraPort;
import io.github.kxyr0.echoscene.api.port.SceneCheckpointStore;
import io.github.kxyr0.echoscene.api.port.SceneExecutionObserver;
import io.github.kxyr0.echoscene.api.port.SceneLeasePort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SceneRuntime {
    private final SceneCommandPort commands;
    private final SceneLeasePort leases;
    private final SceneCameraPort camera;
    private final SceneCheckpointStore checkpoints;
    private final SceneExecutionObserver observer;
    private final Map<SceneExecutionId, SceneExecution> executions = new LinkedHashMap<>();

    public SceneRuntime(SceneCommandPort commands, SceneLeasePort leases, SceneCameraPort camera,
            SceneCheckpointStore checkpoints, SceneExecutionObserver observer) {
        this.commands = commands;
        this.leases = leases;
        this.camera = camera;
        this.checkpoints = checkpoints;
        this.observer = observer;
    }

    public synchronized SceneInspection start(SceneDefinition definition, SceneExecutionId id, String rootOwner) {
        if (executions.containsKey(id)) {
            throw new IllegalArgumentException("Scene execution already exists: " + id.value());
        }
        SceneExecution execution = new SceneExecution(definition, id, rootOwner);
        executions.put(id, execution);
        persist(execution);
        advance(execution);
        return execution.inspection();
    }

    public synchronized SceneInspection recover(SceneDefinition definition, SceneCheckpoint checkpoint) {
        if (!definition.resourceId().equals(checkpoint.resourceId())
                || definition.revision().value() < checkpoint.revision().value()) {
            throw new IllegalArgumentException("SCENE_RECOVERY_FAILED: incompatible Scene definition.");
        }
        SceneExecution execution = SceneExecution.recover(definition, checkpoint);
        executions.put(execution.id(), execution);
        if (!execution.state().terminal()) {
            for (SceneCapability capability : checkpoint.held()) {
                if (!leases.acquire(execution.id(), execution.rootOwner(), capability).acquired()) {
                    fail(execution, "SCENE_RECOVERY_LEASE_FAILED");
                    return execution.inspection();
                }
                execution.held().add(capability);
            }
            advance(execution);
        }
        return execution.inspection();
    }

    public synchronized SceneInspection complete(SceneExecutionId id, SceneStepId step, boolean succeeded,
            String detail) {
        SceneExecution execution = require(id);
        if (!execution.pending().remove(step)) {
            return execution.inspection();
        }
        if (!succeeded) {
            fail(execution, "SCENE_COMMAND_FAILED: " + detail);
            return execution.inspection();
        }
        execution.completed().add(step);
        persist(execution);
        if (execution.pending().isEmpty()) {
            execution.nextStage();
            advance(execution);
        }
        return execution.inspection();
    }

    public synchronized SceneInspection cancel(SceneExecutionId id) {
        SceneExecution execution = require(id);
        if (!execution.state().terminal()) {
            finish(execution, SceneExecutionState.CANCELLED);
        }
        return execution.inspection();
    }

    public synchronized Optional<SceneInspection> inspect(SceneExecutionId id) {
        return Optional.ofNullable(executions.get(id)).map(SceneExecution::inspection);
    }

    public synchronized List<SceneInspection> running() {
        return executions.values().stream().filter(value -> !value.state().terminal())
                .map(SceneExecution::inspection).toList();
    }

    private void advance(SceneExecution execution) {
        while (execution.stageIndex() < execution.definition().stages().size()) {
            SceneStage stage = execution.definition().stages().get(execution.stageIndex());
            if (!acquire(execution, stage)) {
                return;
            }
            execution.state(SceneExecutionState.RUNNING);
            persist(execution);
            if (!dispatch(execution, stage) || !execution.pending().isEmpty()) {
                return;
            }
            execution.nextStage();
        }
        finish(execution, SceneExecutionState.COMPLETED);
    }

    private boolean acquire(SceneExecution execution, SceneStage stage) {
        execution.state(SceneExecutionState.ACQUIRING);
        List<SceneCapability> required = stage.commands().stream().flatMap(value -> value.capabilities().stream())
                .distinct().sorted().filter(value -> !execution.held().contains(value)).toList();
        for (SceneCapability capability : required) {
            SceneLeasePort.LeaseResult result = leases.acquire(execution.id(), execution.rootOwner(), capability);
            if (!result.acquired()) {
                fail(execution, "SCENE_LEASE_ACQUIRE_FAILED: " + result.detail());
                return false;
            }
            execution.held().add(capability);
            persist(execution);
        }
        return true;
    }

    private boolean dispatch(SceneExecution execution, SceneStage stage) {
        for (SceneCommand command : stage.commands()) {
            if (execution.completed().contains(command.stepId())) {
                continue;
            }
            SceneCommandStart result = commands.start(new SceneCommandRequest(execution.definition().resourceId(),
                    execution.definition().entry(), execution.id(), execution.rootOwner(), command));
            if (result.status() == SceneCommandStart.Status.FAILED) {
                fail(execution, "SCENE_COMMAND_FAILED: " + result.detail());
                return false;
            }
            boolean waits = result.status() == SceneCommandStart.Status.WAITING
                    && command.blockingMode() == BlockingMode.DURABLE_BLOCKING;
            if (waits) {
                execution.pending().add(command.stepId());
            } else {
                execution.completed().add(command.stepId());
            }
            persist(execution);
        }
        if (!execution.pending().isEmpty()) {
            execution.state(SceneExecutionState.WAITING);
            persist(execution);
        }
        return true;
    }

    private void fail(SceneExecution execution, String detail) {
        execution.fail(detail);
        execution.cleanup(commands, camera, leases);
        persist(execution);
    }

    private void finish(SceneExecution execution, SceneExecutionState terminal) {
        execution.state(SceneExecutionState.COMPLETING);
        persist(execution);
        execution.cleanup(commands, camera, leases);
        execution.state(terminal);
        persist(execution);
    }

    private void persist(SceneExecution execution) {
        checkpoints.save(execution.checkpoint());
        observer.changed(execution.inspection());
    }

    private SceneExecution require(SceneExecutionId id) {
        SceneExecution execution = executions.get(id);
        if (execution == null) {
            throw new IllegalArgumentException("Scene execution not found: " + id.value());
        }
        return execution;
    }

}
