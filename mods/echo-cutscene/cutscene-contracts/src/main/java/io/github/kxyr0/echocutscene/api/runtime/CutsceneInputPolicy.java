package io.github.kxyr0.echocutscene.api.runtime;

public record CutsceneInputPolicy(
        boolean blockGameplay,
        boolean allowDialogue,
        boolean allowSkip,
        boolean allowPauseMenu
) {
    public static CutsceneInputPolicy defaultPlayback() {
        return new CutsceneInputPolicy(true, true, true, true);
    }
}
