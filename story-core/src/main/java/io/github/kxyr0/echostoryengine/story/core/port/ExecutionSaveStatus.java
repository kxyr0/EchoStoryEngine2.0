package io.github.kxyr0.echostoryengine.story.core.port;

/**
 * Explicit result category for execution checkpoint writes.
 */
public enum ExecutionSaveStatus {
    SAVED,
    REVISION_CONFLICT,
    FAILED
}
