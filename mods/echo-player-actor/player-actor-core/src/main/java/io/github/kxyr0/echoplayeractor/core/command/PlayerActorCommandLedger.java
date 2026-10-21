package io.github.kxyr0.echoplayeractor.core.command;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerActorCommandLedger {
    private final Map<PlayerActorCommandId, PlayerActorCommandRecord> records = new HashMap<>();

    public Optional<PlayerActorResult> replay(PlayerActorCommandRecord candidate) {
        PlayerActorCommandRecord existing = records.get(candidate.commandId());
        if (existing == null) {
            return Optional.empty();
        }
        if (!existing.fingerprint().equals(candidate.fingerprint())) {
            return Optional.of(PlayerActorResult.rejected(
                    PlayerActorFailure.PLAYER_ACTOR_COMMAND_IDEMPOTENCY_CONFLICT,
                    "Command id was reused with a different fingerprint."));
        }
        return Optional.of(new PlayerActorResult(PlayerActorStatus.DUPLICATE, existing.result().outcome(),
                Optional.empty(), existing.result().snapshot(), "Duplicate command replayed."));
    }

    public PlayerActorResult save(PlayerActorCommandRecord record) {
        records.put(record.commandId(), record);
        return new PlayerActorResult(PlayerActorStatus.ACCEPTED, PlayerActorOutcome.NONE, Optional.empty(),
                Optional.empty(), "Command record saved.");
    }
}
