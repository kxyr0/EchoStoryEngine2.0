package io.github.kxyr0.echoplayersleep.core.arm;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepArmId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSleepArmRegistry {
    private final Map<PlayerRef, PlayerSleepArmSnapshot> activeByPlayer = new LinkedHashMap<>();
    private final Map<PlayerSleepCommandId, PlayerSleepArmSnapshot> commandLedger = new LinkedHashMap<>();
    private long generation;

    public PlayerSleepArmDecision arm(PlayerSleepArmRequest request) {
        Objects.requireNonNull(request, "request");
        PlayerSleepArmSnapshot replay = commandLedger.get(request.commandId());
        if (replay != null) {
            return sameCommand(request, replay);
        }
        PlayerSleepArmSnapshot active = activeByPlayer.get(request.player());
        if (active != null && !active.consumed()) {
            return PlayerSleepArmDecision.rejected(PlayerSleepDiagnostic.error(
                    PlayerSleepFailure.PLAYER_SLEEP_ALREADY_ARMED,
                    request.player().uuid().toString(),
                    "Player already has an active sleep arm."));
        }
        PlayerSleepArmSnapshot created = new PlayerSleepArmSnapshot(
                PlayerSleepArmId.random(),
                request.player(),
                request.scenarioId(),
                request.owner(),
                request.commandId(),
                PlayerSleepRevision.initial(),
                ++generation,
                request.createdTick(),
                false);
        activeByPlayer.put(request.player(), created);
        commandLedger.put(request.commandId(), created);
        return PlayerSleepArmDecision.accepted(created);
    }

    public Optional<PlayerSleepArmSnapshot> activeArm(PlayerRef player) {
        return Optional.ofNullable(activeByPlayer.get(Objects.requireNonNull(player, "player")))
                .filter(arm -> !arm.consumed());
    }

    public boolean isArmed(PlayerRef player, PlayerSleepScenarioId scenarioId) {
        return activeArm(player).map(PlayerSleepArmSnapshot::scenarioId)
                .filter(scenarioId::equals).isPresent();
    }

    public Optional<PlayerSleepArmSnapshot> consume(PlayerRef player) {
        PlayerSleepArmSnapshot current = activeByPlayer.get(Objects.requireNonNull(player, "player"));
        if (current == null || current.consumed()) {
            return Optional.empty();
        }
        PlayerSleepArmSnapshot consumed = current.markConsumed();
        activeByPlayer.put(player, consumed);
        commandLedger.put(consumed.commandId(), consumed);
        return Optional.of(consumed);
    }

    public void complete(PlayerRef player) {
        activeByPlayer.remove(Objects.requireNonNull(player, "player"));
    }

    public boolean disarm(PlayerRef player) {
        return activeByPlayer.remove(Objects.requireNonNull(player, "player")) != null;
    }

    private PlayerSleepArmDecision sameCommand(PlayerSleepArmRequest request, PlayerSleepArmSnapshot replay) {
        if (replay.player().equals(request.player()) && replay.scenarioId().equals(request.scenarioId())
                && replay.owner().equals(request.owner())) {
            return PlayerSleepArmDecision.accepted(replay);
        }
        return PlayerSleepArmDecision.rejected(PlayerSleepDiagnostic.error(
                PlayerSleepFailure.PLAYER_SLEEP_ARM_IDEMPOTENCY_CONFLICT,
                request.commandId().value(),
                "Durable command id was reused with different arm data."));
    }
}
