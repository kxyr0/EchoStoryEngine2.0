package io.github.kxyr0.echoscene.api.model;

public enum SceneExecutionState {
    CREATED,
    ACQUIRING,
    RUNNING,
    WAITING,
    COMPLETING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
