package io.github.kxyr0.echoplayeractor.api.control;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import java.util.List;
import java.util.Objects;

public record PlayerControlLease(
        PlayerControlLeaseId leaseId,
        PlayerActorId actorId,
        PlayerControlScopeId scopeId,
        PlayerControlMode mode,
        PlayerActorOwnerId rootOwner,
        List<PlayerActorOwnerId> authorisedOwners,
        long createdTick,
        long generation,
        PlayerControlStatus status,
        long revision) {
    public PlayerControlLease {
        leaseId = Objects.requireNonNull(leaseId, "leaseId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        scopeId = Objects.requireNonNull(scopeId, "scopeId");
        mode = Objects.requireNonNull(mode, "mode");
        rootOwner = Objects.requireNonNull(rootOwner, "rootOwner");
        authorisedOwners = List.copyOf(authorisedOwners);
        status = Objects.requireNonNull(status, "status");
        if (createdTick < 0L || generation < 0L || revision < 0L) {
            throw new IllegalArgumentException("Lease ticks and revisions must not be negative.");
        }
    }

    public boolean authorises(PlayerActorOwnerId ownerId) {
        return rootOwner.equals(ownerId) || authorisedOwners.contains(ownerId);
    }
}
