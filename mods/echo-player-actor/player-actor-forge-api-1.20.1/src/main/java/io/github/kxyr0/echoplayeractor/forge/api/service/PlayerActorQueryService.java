package io.github.kxyr0.echoplayeractor.forge.api.service;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.forge.api.view.PlayerActorSnapshotView;
import java.util.Optional;

public interface PlayerActorQueryService {
    Optional<PlayerActorSnapshotView> snapshot(PlayerActorId actorId);

    boolean isMoving(PlayerActorId actorId);

    boolean isSitting(PlayerActorId actorId);

    boolean isUsingItem(PlayerActorId actorId);

    int selectedHotbarSlot(PlayerActorId actorId);
}
