package io.github.kxyr0.echostoryengine.story.core.port.persistence;

/**
 * Explicit result category for execution checkpoint reads.
 */
public enum ExecutionLoadStatus {
    FOUND,
    NOT_FOUND,
    FAILED
}
