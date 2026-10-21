package io.github.kxyr0.echodevscenarios.runtime;

import io.github.kxyr0.echodevscenarios.api.ManagedStateSnapshot;
import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionState;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import java.util.Optional;

final class DevScenarioSession {
    private final ScenarioDefinition definition;
    private final ScenarioSessionId id;
    private ScenarioSessionState state = ScenarioSessionState.VALIDATING;
    private ManagedStateSnapshot baseline;
    private int cursor;
    private ScenarioStepId waiting;
    private String targetExecution;
    private String failure;

    DevScenarioSession(ScenarioDefinition definition, ScenarioSessionId id) {
        this.definition = definition;
        this.id = id;
    }

    ScenarioDefinition definition() { return definition; }
    ScenarioSessionId id() { return id; }
    ScenarioSessionState state() { return state; }
    void state(ScenarioSessionState value) { state = value; }
    ManagedStateSnapshot baseline() { return baseline; }
    void baseline(ManagedStateSnapshot value) { baseline = value; }
    int cursor() { return cursor; }
    void advance() { cursor++; waiting = null; }
    void reset() { cursor = 0; waiting = null; targetExecution = null; failure = null; }
    ScenarioStepId waiting() { return waiting; }
    void waiting(ScenarioStepId value) { waiting = value; }
    void targetExecution(String value) { targetExecution = value; }
    void fail(String detail) { failure = detail; state = ScenarioSessionState.FAILED; }

    ScenarioInspection inspection() {
        return new ScenarioInspection(definition.id(), id, state, definition.target(), baseline != null,
                Optional.ofNullable(waiting), Optional.ofNullable(targetExecution), Optional.ofNullable(failure));
    }
}
