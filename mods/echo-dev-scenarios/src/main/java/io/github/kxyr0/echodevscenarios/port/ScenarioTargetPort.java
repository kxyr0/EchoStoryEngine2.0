package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;

public interface ScenarioTargetPort {
    LaunchResult launch(ScenarioSessionId owner, ScenarioTarget target);

    void stopOwned(ScenarioSessionId owner);

    record LaunchResult(boolean launched, String executionId, String detail) {
        public static LaunchResult started(String executionId) { return new LaunchResult(true, executionId, ""); }
        public static LaunchResult failed(String detail) { return new LaunchResult(false, "", detail); }
    }
}
