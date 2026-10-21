package io.github.kxyr0.echoplayeractor.forge.api.interaction;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;

public interface PlayerActorInteractionService {
    PlayerActorResult interact(PlayerActorInteractionRequest request);
}
