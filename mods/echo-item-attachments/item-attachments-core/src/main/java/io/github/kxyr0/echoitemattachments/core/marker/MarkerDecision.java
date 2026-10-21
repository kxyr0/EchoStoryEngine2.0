package io.github.kxyr0.echoitemattachments.core.marker;

public record MarkerDecision(Status status, String diagnosticCode) {
    public enum Status {
        ACCEPTED,
        DUPLICATE,
        STALE,
        MISMATCH,
        NO_ACTIVE_BINDING
    }
}
