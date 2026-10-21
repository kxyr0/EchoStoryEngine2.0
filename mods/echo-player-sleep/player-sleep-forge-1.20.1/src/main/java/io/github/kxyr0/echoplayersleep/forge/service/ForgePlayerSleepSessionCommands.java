package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmRegistry;
import io.github.kxyr0.echoplayersleep.core.session.InMemoryPlayerSleepSessionRepository;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepSessionTransition;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import io.github.kxyr0.echoplayersleep.forge.api.recovery.PlayerSleepRecoveryService;
import io.github.kxyr0.echoplayersleep.forge.api.session.PlayerSleepSessionService;
import io.github.kxyr0.echoplayersleep.forge.api.transition.PlayerSleepTransitionClientService;
import io.github.kxyr0.echoplayersleep.forge.network.PlayerSleepNetwork;
import io.github.kxyr0.echoplayersleep.forge.service.dream.ForgeDreamSessionFlow;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePlayerSleepSessionCommands implements PlayerSleepSessionService,
        PlayerSleepTransitionClientService, PlayerSleepRecoveryService {
    private final InMemoryPlayerSleepSessionRepository sessions;
    private final PlayerSleepSessionTransition transition;
    private final PlayerSleepArmRegistry arms;
    private final List<PlayerSleepDiagnostic> diagnostics;
    private final ForgePlayerSleepLifecyclePublisher publisher;
    private final ForgePlayerSleepPlacement placement;
    private final ForgeDreamSessionFlow dreams;

    public ForgePlayerSleepSessionCommands(Supplier<MinecraftServer> server,
                                           InMemoryPlayerSleepSessionRepository sessions,
                                           PlayerSleepSessionTransition transition,
                                           PlayerSleepArmRegistry arms,
                                           List<PlayerSleepDiagnostic> diagnostics,
                                           ForgePlayerSleepLifecyclePublisher publisher) {
        this.sessions = sessions;
        this.transition = transition;
        this.arms = arms;
        this.diagnostics = diagnostics;
        this.publisher = publisher;
        placement = new ForgePlayerSleepPlacement(server);
        dreams = new ForgeDreamSessionFlow(sessions, transition, diagnostics, publisher, placement);
    }

    @Override
    public PlayerSleepResult closeEyes(PlayerRef player, PlayerSleepCommandId commandId, Ticks duration) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty()) {
            return notActive(player);
        }
        Optional<PlayerSleepSessionSnapshot> next = closeStatus(current.orElseThrow());
        if (next.isEmpty()) {
            return ForgePlayerSleepFailures.fail(diagnostics, PlayerSleepOutcome.SKIPPED,
                    PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_REQUIRED,
                    player.uuid().toString(), "CloseEyes requires a claimed sleep story session.");
        }
        PlayerSleepSessionSnapshot snapshot = next.orElseThrow();
        sessions.save(snapshot);
        PlayerSleepRevision revision = snapshot.revision();
        placement.player(player).ifPresent(target -> closeEyes(target, revision, duration));
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult waitUntilClosed(PlayerRef player, PlayerSleepCommandId commandId) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty()) {
            return notActive(player);
        }
        PlayerSleepStatus status = current.orElseThrow().status();
        if (status != PlayerSleepStatus.CLOSING_EYES && status != PlayerSleepStatus.CLOSING_FOR_RETURN) {
            return notActive(player);
        }
        PlayerSleepSessionSnapshot moved = transition.move(current.orElseThrow(), PlayerSleepStatus.EYES_CLOSED);
        sessions.save(moved);
        publish(PlayerSleepLifecyclePhase.EYES_CLOSED, moved);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult enterDream(PlayerRef player, PlayerSleepCommandId commandId,
                                        PlayerSleepDreamDestination destination) {
        return dreams.enterDream(player, commandId, destination); }

    @Override
    public PlayerSleepResult beginBlackspaceDream(PlayerRef player, PlayerSleepCommandId commandId, String instanceId) {
        return dreams.beginBlackspaceDream(player, commandId, instanceId); }

    @Override
    public PlayerSleepResult completeBlackspaceDream(PlayerRef player, PlayerSleepCommandId commandId) {
        return dreams.completeBlackspaceDream(player, commandId); }

    @Override
    public PlayerSleepResult openEyes(PlayerRef player, PlayerSleepCommandId commandId,
                                      Ticks duration, boolean sleepyBlink, boolean showHud) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty()) {
            return notActive(player);
        }
        PlayerSleepSessionSnapshot snapshot = openStatus(current.orElseThrow());
        sessions.save(snapshot);
        PlayerSleepRevision revision = snapshot.revision();
        placement.player(player).ifPresent(target -> openEyes(target, revision, duration, sleepyBlink, showHud));
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult waitUntilOpen(PlayerRef player, PlayerSleepCommandId commandId) {
        return sessions.activeFor(player).isPresent() ? PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED)
                : notActive(player);
    }

    @Override
    public PlayerSleepResult returnToBed(PlayerRef player, PlayerSleepCommandId commandId) {
        return dreams.returnToBed(player, commandId); }

    @Override
    public PlayerSleepResult beginBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId) {
        return dreams.beginBlackspaceReturn(player, commandId); }

    @Override
    public PlayerSleepResult completeBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId) {
        return dreams.completeBlackspaceReturn(player, commandId); }

    @Override
    public PlayerSleepResult completeNight(PlayerRef player, PlayerSleepCommandId commandId) {
        sessions.activeFor(player).ifPresent(placement::completeNight);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult complete(PlayerRef player, PlayerSleepCommandId commandId) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.activeFor(player);
        if (current.isEmpty()) {
            return notActive(player);
        }
        PlayerSleepSessionSnapshot completed = transition.move(current.orElseThrow(), PlayerSleepStatus.COMPLETED);
        sessions.save(completed);
        arms.complete(player);
        publish(PlayerSleepLifecyclePhase.COMPLETED, completed);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult cancel(PlayerRef player, PlayerSleepCommandId commandId) {
        sessions.activeFor(player).ifPresent(snapshot -> {
            if (snapshot.ownsBedPose()) {
                placement.player(player).ifPresent(placement::releaseBedPose);
            }
            sessions.save(transition.force(transition.bedPose(snapshot, false), PlayerSleepStatus.CANCELLED));
        });
        arms.complete(player);
        return PlayerSleepResult.success(PlayerSleepOutcome.CANCELLED);
    }

    @Override
    public void closeEyes(ServerPlayer player, PlayerSleepRevision revision, Ticks duration) {
        PlayerSleepNetwork.closeEyes(player, revision, duration);
    }

    @Override
    public void openEyes(ServerPlayer player, PlayerSleepRevision revision,
                         Ticks duration, boolean sleepyBlink, boolean showHud) {
        PlayerSleepNetwork.openEyes(player, revision, duration, sleepyBlink, showHud);
    }

    @Override
    public PlayerSleepResult recover(PlayerRef player) {
        return sessions.activeFor(player).map(snapshot -> {
            PlayerSleepSessionSnapshot recovered = transition.force(snapshot, PlayerSleepStatus.RECOVERING);
            sessions.save(recovered);
            publish(PlayerSleepLifecyclePhase.RECOVERED, recovered);
            return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
        }).orElseGet(() -> notActive(player));
    }

    private Optional<PlayerSleepSessionSnapshot> closeStatus(PlayerSleepSessionSnapshot snapshot) {
        if (snapshot.status() == PlayerSleepStatus.STORY_CLAIMED) {
            return Optional.of(transition.move(snapshot, PlayerSleepStatus.CLOSING_EYES));
        }
        if (snapshot.status() == PlayerSleepStatus.DREAMING) {
            return Optional.of(transition.move(snapshot, PlayerSleepStatus.CLOSING_FOR_RETURN));
        }
        return Optional.empty();
    }

    private PlayerSleepSessionSnapshot openStatus(PlayerSleepSessionSnapshot snapshot) {
        if (snapshot.status() == PlayerSleepStatus.EYES_CLOSED) {
            PlayerSleepStatus target = snapshot.dreamDestination().isPresent()
                    ? PlayerSleepStatus.DREAMING
                    : PlayerSleepStatus.STORY_CLAIMED;
            return transition.move(snapshot, target);
        }
        if (snapshot.status() == PlayerSleepStatus.RETURNED_AWAKE
                || snapshot.status() == PlayerSleepStatus.RETURNED_SLEEPING) {
            return transition.move(snapshot, PlayerSleepStatus.OPENING_EYES);
        }
        return snapshot;
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
        return ForgePlayerSleepFailures.notActive(diagnostics, player);
    }

    private void publish(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        publisher.publish(phase, snapshot);
    }
}
