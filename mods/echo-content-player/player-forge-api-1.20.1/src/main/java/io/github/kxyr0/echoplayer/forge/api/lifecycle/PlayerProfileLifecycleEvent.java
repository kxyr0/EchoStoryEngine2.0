package io.github.kxyr0.echoplayer.forge.api.lifecycle;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileRevision;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable public lifecycle event payload.
 */
public record PlayerProfileLifecycleEvent(
        EchoPlayerId playerId,
        String phase,
        PlayerProfileRevision profileRevision,
        Instant occurredAt
) {
    public PlayerProfileLifecycleEvent {
        playerId = Objects.requireNonNull(playerId, "playerId");
        phase = Objects.requireNonNull(phase, "phase");
        profileRevision = Objects.requireNonNull(profileRevision, "profileRevision");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
