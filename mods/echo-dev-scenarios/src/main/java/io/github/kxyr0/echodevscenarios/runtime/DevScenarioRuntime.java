package io.github.kxyr0.echodevscenarios.runtime;

import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionState;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.definition.MutationPolicy;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.definition.ScenarioSetupStep;
import io.github.kxyr0.echodevscenarios.port.ManagedStatePort;
import io.github.kxyr0.echodevscenarios.port.ScenarioEventPort;
import io.github.kxyr0.echodevscenarios.port.ScenarioSchemaGate;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echodevscenarios.port.ScenarioTargetPort;
import io.github.kxyr0.echodevscenarios.port.ScenarioValidationGate;
import java.util.Optional;

public final class DevScenarioRuntime {
    private final ScenarioSchemaGate schema;
    private final ScenarioValidationGate validation;
    private final ManagedStatePort managedState;
    private final ScenarioSetupPort setup;
    private final ScenarioTargetPort targets;
    private final ScenarioEventPort events;
    private DevScenarioSession current;

    public DevScenarioRuntime(ScenarioSchemaGate schema, ScenarioValidationGate validation,
            ManagedStatePort managedState, ScenarioSetupPort setup, ScenarioTargetPort targets,
            ScenarioEventPort events) {
        this.schema = schema;
        this.validation = validation;
        this.managedState = managedState;
        this.setup = setup;
        this.targets = targets;
        this.events = events;
    }

    public synchronized ScenarioInspection start(ScenarioDefinition definition, ScenarioSessionId sessionId) {
        if (current != null && current.state() != ScenarioSessionState.STOPPED) stop();
        current = new DevScenarioSession(definition, sessionId);
        publish();
        Optional<String> problem = validation.firstError(definition.id());
        if (problem.isPresent()) return fail("DEV_SCENARIO_INVALID: " + problem.orElseThrow());
        if (!schema.current()) return fail("DEV_SCENARIO_SAVE_SCHEMA_INVALID");
        current.state(ScenarioSessionState.SNAPSHOTTING);
        publish();
        current.baseline(managedState.capture(sessionId));
        apply();
        return current.inspection();
    }

    public synchronized ScenarioInspection complete(ScenarioStepId step, boolean succeeded, String detail) {
        requireCurrent();
        if (!step.equals(current.waiting())) return current.inspection();
        if (!succeeded) return failAndRestore("DEV_SCENARIO_SETUP_FAILED: " + detail);
        current.advance();
        apply();
        return current.inspection();
    }

    public synchronized ScenarioInspection restart() {
        requireCurrent();
        targets.stopOwned(current.id());
        current.state(ScenarioSessionState.RESTORING);
        publish();
        managedState.restore(current.id(), current.baseline());
        current.reset();
        apply();
        return current.inspection();
    }

    public synchronized ScenarioInspection stop() {
        requireCurrent();
        targets.stopOwned(current.id());
        current.state(ScenarioSessionState.RESTORING);
        publish();
        if (current.baseline() != null) managedState.restore(current.id(), current.baseline());
        setup.releaseOwner(current.id());
        current.state(ScenarioSessionState.STOPPED);
        publish();
        return current.inspection();
    }

    public synchronized Optional<ScenarioInspection> status() {
        return Optional.ofNullable(current).map(DevScenarioSession::inspection);
    }

    private void apply() {
        current.state(ScenarioSessionState.APPLYING);
        publish();
        while (current.cursor() < current.definition().setup().size()) {
            ScenarioSetupStep step = current.definition().setup().get(current.cursor());
            ScenarioSetupPort.StartResult result = setup.start(new ScenarioSetupRequest(current.id(),
                    MutationPolicy.SETUP_NO_SIDE_EFFECTS, "SKIP_CROSSED", step));
            if (result.status() == ScenarioSetupPort.Status.FAILED) {
                failAndRestore("DEV_SCENARIO_SETUP_FAILED: " + result.detail());
                return;
            }
            if (result.status() == ScenarioSetupPort.Status.WAITING && step.blocking()) {
                current.waiting(step.id());
                current.state(ScenarioSessionState.WAITING);
                publish();
                return;
            }
            current.advance();
        }
        launch();
    }

    private void launch() {
        current.state(ScenarioSessionState.READY);
        publish();
        ScenarioTargetPort.LaunchResult result = targets.launch(current.id(), current.definition().target());
        setup.releaseOwner(current.id());
        if (!result.launched()) {
            failAndRestore("DEV_SCENARIO_TARGET_FAILED: " + result.detail());
            return;
        }
        current.targetExecution(result.executionId());
        current.state(ScenarioSessionState.PLAYING);
        publish();
    }

    private ScenarioInspection failAndRestore(String detail) {
        targets.stopOwned(current.id());
        setup.releaseOwner(current.id());
        if (current.baseline() != null) managedState.restore(current.id(), current.baseline());
        return fail(detail);
    }

    private ScenarioInspection fail(String detail) {
        current.fail(detail);
        publish();
        return current.inspection();
    }

    private void publish() {
        events.changed(current.inspection());
    }

    private void requireCurrent() {
        if (current == null) throw new IllegalStateException("DEV_SCENARIO_NOT_FOUND");
    }
}
