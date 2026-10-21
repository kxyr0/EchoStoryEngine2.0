package io.github.kxyr0.echodevscenarios.api;

import java.util.Objects;
import java.util.Optional;

public record ScenarioInspection(
        ScenarioId scenario,
        ScenarioSessionId session,
        ScenarioSessionState state,
        ScenarioTarget target,
        boolean baselineAvailable,
        Optional<ScenarioStepId> waitingStep,
        Optional<String> targetExecution,
        Optional<String> failure
) {
    public ScenarioInspection {
        scenario = Objects.requireNonNull(scenario, "scenario");
        session = Objects.requireNonNull(session, "session");
        state = Objects.requireNonNull(state, "state");
        target = Objects.requireNonNull(target, "target");
        waitingStep = Objects.requireNonNull(waitingStep, "waitingStep");
        targetExecution = Objects.requireNonNull(targetExecution, "targetExecution");
        failure = Objects.requireNonNull(failure, "failure");
    }
}
