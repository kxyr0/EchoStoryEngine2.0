package io.github.kxyr0.echoplayeractor.forge.api.service;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;

public interface PlayerActorService {
    PlayerActorResult spawnAt(PlayerActorSpawnRequest request);

    PlayerActorResult setHotbarItem(PlayerHotbarItemRequest request);

    PlayerActorResult selectHotbarSlot(PlayerHotbarSelectionRequest request);

    PlayerActorResult sitAt(PlayerSitRequest request);

    PlayerActorResult rotate(PlayerActorRotationRequest request);

    PlayerActorResult lookAt(PlayerActorLookRequest request);

    PlayerActorResult move(PlayerActorMoveRequest request);

    PlayerActorResult stop(PlayerActorId actorId, PlayerActorOwnerId ownerId);

    PlayerActorResult interact(PlayerActorInteractionRequest request);

    PlayerActorResult standUp(PlayerActorId actorId, PlayerActorOwnerId ownerId);

    PlayerActorResult stopUsingItem(PlayerActorId actorId, PlayerActorOwnerId ownerId);
}
