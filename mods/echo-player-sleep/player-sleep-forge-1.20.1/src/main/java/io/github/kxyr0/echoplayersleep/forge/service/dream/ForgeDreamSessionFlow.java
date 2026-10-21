package io.github.kxyr0.echoplayersleep.forge.service.dream;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.core.session.InMemoryPlayerSleepSessionRepository;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepSessionTransition;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepLifecyclePublisher;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepPlacement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeDreamSessionFlow {
    private final InMemoryPlayerSleepSessionRepository sessions;
    private final PlayerSleepSessionTransition transition;
    private final List<PlayerSleepDiagnostic> diagnostics;
    private final ForgePlayerSleepLifecyclePublisher publisher;
    private final ForgePlayerSleepPlacement placement;

    public ForgeDreamSessionFlow(
            InMemoryPlayerSleepSessionRepository sessions,
            PlayerSleepSessionTransition transition,
            List<PlayerSleepDiagnostic> diagnostics,
            ForgePlayerSleepLifecyclePublisher publisher,
            ForgePlayerSleepPlacement placement) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.transition = Objects.requireNonNull(transition, "transition");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.placement = Objects.requireNonNull(placement, "placement");
    }

    public PlayerSleepResult enterDream(
            PlayerRef player,
            PlayerSleepCommandId commandId,
            PlayerSleepDreamDestination destination) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        Optional<ServerPlayer> target = placement.player(player);
        if (current.isEmpty() || target.isEmpty()
                || current.orElseThrow().status() != PlayerSleepStatus.EYES_CLOSED) {
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_DREAM_DESTINATION_INVALID, player.uuid().toString(),
                    "Dream destination is unavailable or eyes are not closed.");
        }
        if (!placement.canEnterDream(destination)) {
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_DREAM_DIMENSION_INVALID, destination.dimension().value(),
                    "Dream dimension is not loaded.");
        }
        PlayerSleepSessionSnapshot entering = transition.dream(current.orElseThrow(), destination);
        sessions.save(entering);
        if (!placement.releaseBedPose(target.orElseThrow())) {
            sessions.save(transition.force(entering, PlayerSleepStatus.FAILED));
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_DREAM_ENTRY_FAILED, player.uuid().toString(),
                    "Vanilla bed pose did not release before dream teleport.");
        }
        PlayerSleepSessionSnapshot released = transition.bedPose(entering, false);
        sessions.save(released);
        if (!placement.teleportToDream(target.orElseThrow(), destination)) {
            sessions.save(transition.force(released, PlayerSleepStatus.FAILED));
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_DREAM_DIMENSION_INVALID, destination.dimension().value(),
                    "Dream dimension is not loaded.");
        }
        PlayerSleepSessionSnapshot dreaming = transition.move(released, PlayerSleepStatus.DREAMING);
        sessions.save(dreaming);
        publish(PlayerSleepLifecyclePhase.DREAM_ENTERED, dreaming);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    public PlayerSleepResult beginBlackspaceDream(
            PlayerRef player,
            PlayerSleepCommandId commandId,
            String instanceId) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        Optional<ServerPlayer> target = placement.player(player);
        if (current.isEmpty() || target.isEmpty()
                || current.orElseThrow().status() != PlayerSleepStatus.EYES_CLOSED) {
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_EYES_NOT_CLOSED, player.uuid().toString(),
                    "Blackspace dream entry requires closed eyes and an online player.");
        }
        PlayerSleepSessionSnapshot entering = transition.move(current.orElseThrow(),
                PlayerSleepStatus.ENTERING_DREAM);
        sessions.save(entering);
        if (!placement.releaseBedPose(target.orElseThrow())) {
            sessions.save(transition.force(entering, PlayerSleepStatus.FAILED));
            return fail(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                    PlayerSleepFailure.PLAYER_SLEEP_DREAM_ENTRY_FAILED, player.uuid().toString(),
                    "Vanilla bed pose did not release before Blackspace entry.");
        }
        sessions.save(transition.bedPose(entering, false));
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    public PlayerSleepResult completeBlackspaceDream(PlayerRef player, PlayerSleepCommandId commandId) {
        return moveIf(player, PlayerSleepStatus.ENTERING_DREAM,
                PlayerSleepStatus.DREAMING, PlayerSleepLifecyclePhase.DREAM_ENTERED);
    }

    public PlayerSleepResult returnToBed(PlayerRef player, PlayerSleepCommandId commandId) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        Optional<ServerPlayer> target = placement.player(player);
        if (current.isEmpty() || target.isEmpty()) {
            return notActive(player);
        }
        PlayerSleepSessionSnapshot returning = transition.move(current.orElseThrow(), PlayerSleepStatus.RETURNING);
        if (!placement.returnToBed(target.orElseThrow(), returning)) {
            return fail(PlayerSleepOutcome.INTERNAL_FAILURE,
                    PlayerSleepFailure.PLAYER_SLEEP_RETURN_ORIGIN_INVALID,
                    returning.bed().dimension().value(), "Return dimension is not loaded.");
        }
        PlayerSleepSessionSnapshot returned = transition.move(returning, PlayerSleepStatus.RETURNED_AWAKE);
        sessions.save(returned);
        publish(PlayerSleepLifecyclePhase.RETURNED, returned);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    public PlayerSleepResult beginBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty()) {
            return notActive(player);
        }
        PlayerSleepStatus status = current.orElseThrow().status();
        if (status != PlayerSleepStatus.CLOSING_FOR_RETURN && status != PlayerSleepStatus.EYES_CLOSED) {
            return fail(PlayerSleepOutcome.INTERNAL_FAILURE, PlayerSleepFailure.PLAYER_SLEEP_RETURN_FAILED,
                    player.uuid().toString(), "Blackspace return requires closed eyes.");
        }
        PlayerSleepSessionSnapshot returning = transition.move(current.orElseThrow(), PlayerSleepStatus.RETURNING);
        sessions.save(returning);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    public PlayerSleepResult completeBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId) {
        return moveIf(player, PlayerSleepStatus.RETURNING,
                PlayerSleepStatus.RETURNED_AWAKE, PlayerSleepLifecyclePhase.RETURNED);
    }

    private PlayerSleepResult moveIf(PlayerRef player, PlayerSleepStatus from,
                                     PlayerSleepStatus to, PlayerSleepLifecyclePhase phase) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty() || current.orElseThrow().status() != from) {
            return notActive(player);
        }
        PlayerSleepSessionSnapshot moved = transition.move(current.orElseThrow(), to);
        sessions.save(moved);
        publish(phase, moved);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    private PlayerSleepResult notActive(PlayerRef player) {
        return fail(PlayerSleepOutcome.SKIPPED, PlayerSleepFailure.PLAYER_SLEEP_NOT_ARMED,
                player.uuid().toString(), "Player has no active sleep session.");
    }

    private PlayerSleepResult fail(
            PlayerSleepOutcome outcome,
            PlayerSleepFailure code,
            String subject,
            String message) {
        PlayerSleepDiagnostic diagnostic = PlayerSleepDiagnostic.error(code, subject, message);
        diagnostics.add(diagnostic);
        return PlayerSleepResult.failure(outcome, diagnostic);
    }

    private void publish(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        publisher.publish(phase, snapshot);
    }
}
