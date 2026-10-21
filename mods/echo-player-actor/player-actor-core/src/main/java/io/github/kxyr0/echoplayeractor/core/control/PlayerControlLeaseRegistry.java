package io.github.kxyr0.echoplayeractor.core.control;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerControlLeaseRegistry {
    private final Map<PlayerActorId, PlayerControlLease> activeByActor = new HashMap<>();

    public PlayerActorResult acquire(PlayerControlLease lease) {
        Optional<PlayerControlLease> current = active(lease.actorId());
        if (current.isPresent() && !sameLease(current.orElseThrow(), lease)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_CONTROL_ALREADY_LEASED,
                    "Player body is already leased.");
        }
        activeByActor.put(lease.actorId(), lease);
        return new PlayerActorResult(PlayerActorStatus.ACCEPTED, PlayerActorOutcome.NONE, Optional.empty(),
                Optional.empty(), "Player control lease acquired.");
    }

    public boolean authorises(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return active(actorId).map(lease -> lease.authorises(ownerId)).orElse(true);
    }

    public Optional<PlayerControlLease> active(PlayerActorId actorId) {
        return Optional.ofNullable(activeByActor.get(actorId))
                .filter(lease -> lease.status() == PlayerControlStatus.ACTIVE);
    }

    public PlayerActorResult release(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        Optional<PlayerControlLease> lease = active(actorId);
        if (lease.isEmpty()) {
            return new PlayerActorResult(PlayerActorStatus.ACCEPTED, PlayerActorOutcome.NONE, Optional.empty(),
                    Optional.empty(), "No active player control lease.");
        }
        if (!lease.orElseThrow().authorises(ownerId)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_CONTROL_OWNER_MISMATCH,
                    "Owner is not authorised to release this lease.");
        }
        activeByActor.remove(actorId);
        return new PlayerActorResult(PlayerActorStatus.ACCEPTED, PlayerActorOutcome.STOPPED, Optional.empty(),
                Optional.empty(), "Player control released.");
    }

    public void emergencyRelease(PlayerActorId actorId) {
        activeByActor.remove(actorId);
    }

    private boolean sameLease(PlayerControlLease first, PlayerControlLease second) {
        return first.leaseId().equals(second.leaseId()) || first.rootOwner().equals(second.rootOwner());
    }

}
