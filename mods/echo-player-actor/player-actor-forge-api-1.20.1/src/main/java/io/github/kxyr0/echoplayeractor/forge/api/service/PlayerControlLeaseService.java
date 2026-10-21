package io.github.kxyr0.echoplayeractor.forge.api.service;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Optional;

public interface PlayerControlLeaseService {
    PlayerActorResult acquire(PlayerControlLease lease);

    PlayerActorResult release(PlayerActorId actorId, PlayerActorOwnerId ownerId);

    void emergencyRelease(PlayerActorId actorId);

    boolean authorises(PlayerActorId actorId, PlayerActorOwnerId ownerId);

    Optional<PlayerControlLease> active(PlayerActorId actorId);
}
