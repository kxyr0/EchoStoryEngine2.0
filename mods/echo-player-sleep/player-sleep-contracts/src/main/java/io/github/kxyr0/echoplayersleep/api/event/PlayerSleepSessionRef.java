package io.github.kxyr0.echoplayersleep.api.event;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import java.util.Objects;

public record PlayerSleepSessionRef(
        PlayerSleepSessionId sessionId,
        PlayerSleepRevision revision) {
    public PlayerSleepSessionRef {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        revision = Objects.requireNonNull(revision, "revision");
    }
}
