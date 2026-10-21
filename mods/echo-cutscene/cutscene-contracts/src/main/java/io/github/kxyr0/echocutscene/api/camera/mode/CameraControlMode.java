package io.github.kxyr0.echocutscene.api.camera.mode;

public enum CameraControlMode {
    FREE,
    LOOK_AT,
    LOOK_AT_WITH_OFFSET;

    public CameraControlMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
