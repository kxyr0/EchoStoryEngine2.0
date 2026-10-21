package io.github.kxyr0.echoplayeractor.script;

import java.util.Objects;

public record PlayerActorScriptCommand(String waitType, String payload, boolean blocking) {
    public PlayerActorScriptCommand {
        waitType = Objects.requireNonNull(waitType, "waitType");
        payload = Objects.requireNonNull(payload, "payload");
    }
}
