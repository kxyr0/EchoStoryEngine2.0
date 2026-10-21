package io.github.kxyr0.echoplayeractor.forge.api.view;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorTransform;

public interface PlayerActorBodyView {
    PlayerActorId actorId();

    PlayerActorTransform transform();

    boolean online();
}
