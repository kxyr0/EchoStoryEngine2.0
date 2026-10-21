package io.github.kxyr0.echovisibility.api.state;

public enum VisibilityTransitionKind {
    NONE,
    FIRST_VISIBLE,
    LOST_SIGHT,
    GAINED_SIGHT,
    TARGET_BECAME_UNAVAILABLE,
    CAMERA_BECAME_UNAVAILABLE
}
