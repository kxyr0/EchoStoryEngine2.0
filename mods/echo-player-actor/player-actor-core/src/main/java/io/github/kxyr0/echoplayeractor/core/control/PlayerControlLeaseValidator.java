package io.github.kxyr0.echoplayeractor.core.control;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public final class PlayerControlLeaseValidator {
    public PlayerActorResult validate(PlayerControlLease lease, PlayerActorOwnerId ownerId) {
        Objects.requireNonNull(lease, "lease");
        Objects.requireNonNull(ownerId, "ownerId");
        if (lease.status() != PlayerControlStatus.ACTIVE) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_CONTROL_UNAVAILABLE,
                    "Player control lease is not active.");
        }
        if (!lease.authorises(ownerId)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_CONTROL_OWNER_MISMATCH,
                    "Owner is not authorised by the active player control lease.");
        }
        return new PlayerActorResult(io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus.ACCEPTED,
                io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome.NONE,
                java.util.Optional.empty(), java.util.Optional.empty(), "Lease owner accepted.");
    }
}
