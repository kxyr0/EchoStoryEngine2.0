package io.github.kxyr0.echoplayervitals.forge.service;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsDiagnostic;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsResult;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.diagnostic.PlayerVitalsDiagnosticCodes;
import io.github.kxyr0.echoplayervitals.core.fatigue.PlayerFatigueSnapshotFactory;
import io.github.kxyr0.echoplayervitals.core.mutation.PlayerFatigueMutation;
import io.github.kxyr0.echoplayervitals.core.mutation.PlayerFatigueMutationResult;
import io.github.kxyr0.echoplayervitals.core.mutation.PlayerVitalsMutationLedger;
import io.github.kxyr0.echoplayervitals.core.repository.PlayerVitalsRepository;
import io.github.kxyr0.echoplayervitals.forge.PlayerVitalsPublication;
import io.github.kxyr0.echoplayervitals.forge.api.EchoPlayerVitalsService;
import io.github.kxyr0.echoplayervitals.forge.api.lifecycle.PlayerVitalsLifecycleReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge-backed public Vitals service implementation.
 */
public final class ForgePlayerVitalsServices implements EchoPlayerVitalsService {
    private final Supplier<PlayerVitalsRepository> repositories;
    private final PlayerVitalsConfig config;
    private final PlayerVitalsMutationLedger ledger;
    private final Consumer<PlayerVitalsPublication> publisher;
    private final PlayerFatigueMutation fatigueMutation = new PlayerFatigueMutation();
    private final PlayerFatigueSnapshotFactory snapshots = new PlayerFatigueSnapshotFactory();
    private final List<PlayerVitalsDiagnostic> diagnostics = new ArrayList<>();

    public ForgePlayerVitalsServices(Supplier<PlayerVitalsRepository> repositories, PlayerVitalsConfig config,
                                     PlayerVitalsMutationLedger ledger,
                                     Consumer<PlayerVitalsPublication> publisher) {
        this.repositories = repositories;
        this.config = config;
        this.ledger = ledger;
        this.publisher = publisher;
    }

    public PlayerVitalsSnapshot ensure(ServerPlayer player, PlayerVitalsLifecycleReason reason) {
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(player.getUUID());
        PlayerVitalsSnapshot snapshot = find(playerId).orElseGet(() -> initial(player));
        PlayerVitalsSnapshot live = withLiveState(snapshot, player, snapshot.revision());
        repositories.get().save(live);
        publisher.accept(new PlayerVitalsPublication(live, reason));
        return live;
    }

    public PlayerVitalsSnapshot snapshotFor(ServerPlayer player) {
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(player.getUUID());
        return find(playerId).map(snapshot -> withLiveState(snapshot, player, snapshot.revision()))
                .orElseGet(() -> ensure(player, PlayerVitalsLifecycleReason.LOGIN));
    }

    @Override
    public Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId) {
        return repositories.get().find(playerId);
    }

    @Override
    public List<PlayerVitalsSnapshot> findAll() {
        return repositories.get().findAll();
    }

    @Override
    public PlayerVitalsResult<PlayerVitalsSnapshot> setFatigue(UUID accountUuid,
                                                               PlayerVitalsMutationId mutationId,
                                                               int current) {
        return mutate(accountUuid, mutationId, fatigue -> fatigueMutation.set(fatigue, current));
    }

    @Override
    public PlayerVitalsResult<PlayerVitalsSnapshot> addFatigue(UUID accountUuid,
                                                               PlayerVitalsMutationId mutationId,
                                                               int amount) {
        return mutate(accountUuid, mutationId, fatigue -> fatigueMutation.add(fatigue, amount));
    }

    @Override
    public PlayerVitalsResult<PlayerVitalsSnapshot> reduceFatigue(UUID accountUuid,
                                                                  PlayerVitalsMutationId mutationId,
                                                                  int amount) {
        return mutate(accountUuid, mutationId, fatigue -> fatigueMutation.reduce(fatigue, amount));
    }

    @Override
    public PlayerVitalsResult<PlayerVitalsSnapshot> restoreFatigue(UUID accountUuid,
                                                                   PlayerVitalsMutationId mutationId) {
        return mutate(accountUuid, mutationId, fatigueMutation::restoreFully);
    }

    @Override
    public List<PlayerVitalsDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    private PlayerVitalsResult<PlayerVitalsSnapshot> mutate(UUID accountUuid, PlayerVitalsMutationId mutationId,
                                                            Function<PlayerFatigue,
                                                                    PlayerFatigueMutationResult> mutation) {
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(accountUuid);
        Optional<PlayerVitalsSnapshot> replay = ledger.replay(playerId, mutationId);
        if (replay.isPresent()) {
            return PlayerVitalsResult.valid(replay.orElseThrow(), List.of());
        }
        Optional<PlayerVitalsSnapshot> current = find(playerId);
        if (current.isEmpty()) {
            PlayerVitalsDiagnostic diagnostic = PlayerVitalsDiagnostic.error(
                    PlayerVitalsDiagnosticCodes.PROFILE_NOT_FOUND, "Vitals profile is not loaded.", playerId.value());
            diagnostics.add(diagnostic);
            return PlayerVitalsResult.invalid(List.of(diagnostic));
        }
        PlayerVitalsSnapshot next = apply(current.orElseThrow(), mutation.apply(current.orElseThrow()
                .fatigue().fatigue()));
        repositories.get().save(next);
        ledger.remember(playerId, mutationId, next);
        publisher.accept(new PlayerVitalsPublication(next, PlayerVitalsLifecycleReason.MUTATION));
        return PlayerVitalsResult.valid(next, List.of());
    }

    private PlayerVitalsSnapshot apply(PlayerVitalsSnapshot current, PlayerFatigueMutationResult result) {
        PlayerVitalsRevision revision = result.changed() ? current.revision().next() : current.revision();
        return build(current.playerId(), result.fatigue(), current.health(), current.hunger(), revision);
    }

    private PlayerVitalsSnapshot initial(ServerPlayer player) {
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(player.getUUID());
        return build(playerId, new PlayerFatigue(0, config.fatigueMaximum()), health(player),
                PlayerHungerSnapshot.neutral(), PlayerVitalsRevision.initial());
    }

    private PlayerVitalsSnapshot withLiveState(PlayerVitalsSnapshot snapshot, ServerPlayer player,
                                              PlayerVitalsRevision revision) {
        return build(snapshot.playerId(), snapshot.fatigue().fatigue(), health(player),
                PlayerHungerSnapshot.neutral(), revision);
    }

    private PlayerVitalsSnapshot build(PlayerVitalsId playerId, PlayerFatigue fatigue,
                                       PlayerHealthSnapshot health, PlayerHungerSnapshot hunger,
                                       PlayerVitalsRevision revision) {
        return new PlayerVitalsSnapshot(playerId, snapshots.snapshot(fatigue, config, revision).value().orElseThrow(),
                health, hunger, revision);
    }

    private PlayerHealthSnapshot health(ServerPlayer player) {
        return new PlayerHealthSnapshot(Math.max(0.0F, player.getHealth()), player.getMaxHealth(),
                player.isAlive());
    }
}
