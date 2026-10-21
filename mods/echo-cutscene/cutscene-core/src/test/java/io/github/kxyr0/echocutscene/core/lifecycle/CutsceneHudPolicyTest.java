package io.github.kxyr0.echocutscene.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import org.junit.jupiter.api.Test;

final class CutsceneHudPolicyTest {
    @Test
    void playbackIsCinematicByDefault() {
        assertTrue(CutsceneHudPolicy.defaultPlayback().cinematic());
        assertTrue(new CutsceneHudPolicy(true, true, false).cinematic());
    }
}
