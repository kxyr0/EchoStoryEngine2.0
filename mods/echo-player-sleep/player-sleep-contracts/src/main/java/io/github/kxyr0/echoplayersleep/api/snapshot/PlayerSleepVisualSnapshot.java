package io.github.kxyr0.echoplayersleep.api.snapshot;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.visual.PlayerSleepVisualProfileId;
import java.util.Objects;

public record PlayerSleepVisualSnapshot(
        PlayerSleepVisualProfileId profileId,
        PlayerSleepRevision revision,
        double coverage,
        boolean closing,
        boolean blackFramePublished,
        boolean sleepyBlink) {
    public PlayerSleepVisualSnapshot {
        profileId = Objects.requireNonNull(profileId, "profileId");
        revision = Objects.requireNonNull(revision, "revision");
        if (!Double.isFinite(coverage) || coverage < 0.0D || coverage > 1.0D) {
            throw new IllegalArgumentException("coverage must be in [0, 1]");
        }
    }
}
