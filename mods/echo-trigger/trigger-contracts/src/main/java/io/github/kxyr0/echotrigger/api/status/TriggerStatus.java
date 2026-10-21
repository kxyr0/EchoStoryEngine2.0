package io.github.kxyr0.echotrigger.api.status;

public enum TriggerStatus {
    WAITING,
    RUNNING,
    COMPLETED,
    STOPPED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == STOPPED || this == FAILED;
    }
}
