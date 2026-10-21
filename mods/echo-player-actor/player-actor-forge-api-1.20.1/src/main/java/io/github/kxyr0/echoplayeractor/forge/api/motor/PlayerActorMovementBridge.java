package io.github.kxyr0.echoplayeractor.forge.api.motor;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;

public interface PlayerActorMovementBridge {
    PlayerActorResult move(PlayerActorMoveRequest request, PlayerActorMotor motor);

    PlayerActorResult stop(PlayerActorId actorId, PlayerActorMotor motor);
}
