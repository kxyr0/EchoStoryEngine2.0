package io.github.kxyr0.echostoryengine.story.core.domain;

/**
 * Minimal node kinds needed by the M1 deterministic story slice.
 */
public enum StoryNodeKind {
    START,
    SET_VARIABLE,
    CONDITION,
    EXTERNAL_ACTION,
    FINISH
}
