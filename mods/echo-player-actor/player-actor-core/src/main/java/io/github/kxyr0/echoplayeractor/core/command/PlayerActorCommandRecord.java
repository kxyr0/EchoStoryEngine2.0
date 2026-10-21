package io.github.kxyr0.echoplayeractor.core.command;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorCommandType;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerActorCommandRecord(
        PlayerActorCommandId commandId,
        PlayerActorCommandType type,
        PlayerActorOwnerId ownerId,
        String fingerprint,
        PlayerActorResult result) {
    public PlayerActorCommandRecord {
        commandId = Objects.requireNonNull(commandId, "commandId");
        type = Objects.requireNonNull(type, "type");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        result = Objects.requireNonNull(result, "result");
    }
}
