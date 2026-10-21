package io.github.kxyr0.echoevents.api.context;

import java.util.Objects;
import java.util.UUID;

public sealed interface EventAudience permits EventAudience.Player, EventAudience.World {
    record Player(UUID playerId) implements EventAudience {
        public Player { Objects.requireNonNull(playerId, "playerId"); }
    }
    record World() implements EventAudience { }
}
