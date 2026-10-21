package io.github.kxyr0.echodialoguecutscene.forge.wait;

import java.util.Arrays;
import java.util.Optional;

enum CutsceneWaitType {
    START("echo_cutscene:start"),
    PLAY("echo_cutscene:play"),
    WAIT_PREPARED("echo_cutscene:wait_prepared"),
    WAIT("echo_cutscene:wait"),
    STOP("echo_cutscene:stop");

    private final String waitType;

    CutsceneWaitType(String waitType) {
        this.waitType = waitType;
    }

    static Optional<CutsceneWaitType> from(String waitType) {
        return Arrays.stream(values())
                .filter(value -> value.waitType.equals(waitType))
                .findFirst();
    }
}
