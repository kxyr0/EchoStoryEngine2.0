package io.github.kxyr0.echocutscene.api.runtime;

public enum CutsceneStatus {
    STARTING,
    PLAYING,
    HOLDING_LAST_FRAME,
    WAITING_FOR_CLIENT,
    COMPLETED,
    STOPPED,
    FAILED
}
