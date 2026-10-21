package io.github.kxyr0.echoplayeractor.forge.api.lifecycle;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorCommandType;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerActorLifecycleEvent extends Event {
    private final PlayerActorId actorId;
    private final Optional<PlayerActorCommandId> commandId;
    private final PlayerActorCommandType type;
    private final PlayerActorResult result;

    public PlayerActorLifecycleEvent(
            PlayerActorId actorId,
            Optional<PlayerActorCommandId> commandId,
            PlayerActorCommandType type,
            PlayerActorResult result
    ) {
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.commandId = Objects.requireNonNull(commandId, "commandId");
        this.type = Objects.requireNonNull(type, "type");
        this.result = Objects.requireNonNull(result, "result");
    }

    public PlayerActorId actorId() {
        return actorId;
    }

    public Optional<PlayerActorCommandId> commandId() {
        return commandId;
    }

    public PlayerActorCommandType type() {
        return type;
    }

    public PlayerActorResult result() {
        return result;
    }
}
