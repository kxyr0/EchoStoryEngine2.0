package io.github.kxyr0.echoplayersleep.core.transition;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import java.util.Objects;

public record ClientTransitionAck(
        PlayerSleepRevision revision,
        boolean blackFramePublished,
        double coverage) {
    public ClientTransitionAck {
        revision = Objects.requireNonNull(revision, "revision");
        if (!Double.isFinite(coverage) || coverage < 0.0D || coverage > 1.0D) {
            throw new IllegalArgumentException("coverage must be in [0, 1]");
        }
    }
}
