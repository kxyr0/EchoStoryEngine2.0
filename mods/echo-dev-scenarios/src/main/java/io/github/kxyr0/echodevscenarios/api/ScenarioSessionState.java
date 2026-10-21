package io.github.kxyr0.echodevscenarios.api;

public enum ScenarioSessionState {
    VALIDATING,
    SNAPSHOTTING,
    APPLYING,
    WAITING,
    READY,
    PLAYING,
    RESTORING,
    STOPPED,
    FAILED
}
