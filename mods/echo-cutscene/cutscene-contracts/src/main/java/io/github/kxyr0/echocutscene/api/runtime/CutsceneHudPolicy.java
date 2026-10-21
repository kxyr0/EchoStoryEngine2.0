package io.github.kxyr0.echocutscene.api.runtime;

public record CutsceneHudPolicy(
        boolean hideGameplayHud,
        boolean keepDialogueVisible,
        boolean showDebugHud,
        boolean cinematic
) {
    public CutsceneHudPolicy(boolean hideGameplayHud, boolean keepDialogueVisible, boolean showDebugHud) {
        this(hideGameplayHud, keepDialogueVisible, showDebugHud, true);
    }

    public static CutsceneHudPolicy defaultPlayback() {
        return new CutsceneHudPolicy(true, true, false, true);
    }
}
