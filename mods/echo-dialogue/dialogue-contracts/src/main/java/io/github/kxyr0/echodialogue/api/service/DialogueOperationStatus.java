package io.github.kxyr0.echodialogue.api.service;

/**
 * Public result category for Echo Dialogue service operations.
 */
public enum DialogueOperationStatus {
    ACCEPTED,
    STARTED,
    ALREADY_ACTIVE,
    WAITING,
    ALREADY_TERMINAL,
    NOT_FOUND,
    REJECTED,
    BLOCKED,
    CONFLICT,
    SERVICE_UNAVAILABLE,
    FAILED
}
