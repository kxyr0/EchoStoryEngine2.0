package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;

public interface ScenarioSetupPort {
    StartResult start(ScenarioSetupRequest request);

    void releaseOwner(ScenarioSessionId owner);

    record StartResult(Status status, String detail) {
        public static StartResult completed() { return new StartResult(Status.COMPLETED, ""); }
        public static StartResult waiting(String receipt) { return new StartResult(Status.WAITING, receipt); }
        public static StartResult failed(String detail) { return new StartResult(Status.FAILED, detail); }
    }

    enum Status {
        COMPLETED,
        WAITING,
        FAILED
    }
}
