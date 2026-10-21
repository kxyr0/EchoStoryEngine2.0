package io.github.kxyr0.echovisibility.api.state;

public enum VisibilityState {
    UNKNOWN,
    VISIBLE,
    PARTIALLY_VISIBLE,
    OCCLUDED,
    OUTSIDE_FRUSTUM,
    BEHIND_CAMERA,
    TARGET_UNAVAILABLE,
    CAMERA_UNAVAILABLE,
    CONCEALED_PENDING_SERVER;

    public boolean visible() {
        return this == VISIBLE || this == PARTIALLY_VISIBLE;
    }

    public boolean geometricallyHidden() {
        return this == OCCLUDED || this == OUTSIDE_FRUSTUM || this == BEHIND_CAMERA;
    }
}
