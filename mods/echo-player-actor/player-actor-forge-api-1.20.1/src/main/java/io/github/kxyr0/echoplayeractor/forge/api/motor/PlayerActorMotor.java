package io.github.kxyr0.echoplayeractor.forge.api.motor;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;

public interface PlayerActorMotor {
    PlayerActorResult start(PlayerActorMoveRequest request);

    PlayerActorResult stop(PlayerActorId actorId);

    boolean isMoving(PlayerActorId actorId);
}
