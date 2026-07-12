package io.github.kxyr0.echostoryengine.story.api;

/**
 * Explicit lifecycle phase for one story execution.
 */
public enum StoryExecutionPhase {
    READY,
    RUNNING,
    WAITING,
    FINISHED,
    FAILED
}
