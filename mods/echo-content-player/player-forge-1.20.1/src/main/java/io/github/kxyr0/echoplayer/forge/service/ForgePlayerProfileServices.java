package io.github.kxyr0.echoplayer.forge.service;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.identity.PlayerIdentitySnapshot;
import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import io.github.kxyr0.echoplayer.api.spark.PlayerSparkSnapshot;
import io.github.kxyr0.echoplayer.core.dialogue.PlayerDialogueCallBinder;
import io.github.kxyr0.echoplayer.core.mutation.PlayerMutationLedger;
import io.github.kxyr0.echoplayer.core.repository.PlayerProfileRepository;
import io.github.kxyr0.echoplayer.core.spark.MaximumSparkPolicy;
import io.github.kxyr0.echoplayer.core.spark.PlayerSparkMutation;
import io.github.kxyr0.echoplayer.core.spark.SparkMutationResult;
import io.github.kxyr0.echoplayer.core.spark.SparkMutationStatus;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import io.github.kxyr0.echoplayer.forge.api.EchoPlayerService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerDialogueMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerAppearanceMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerIdentityMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerSparkMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerTemplateService;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import io.github.kxyr0.echoplayer.forge.log.PlayerStructuredLogger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;

/**
 * Server-authoritative service implementation for the public Forge API.
 */
public final class ForgePlayerProfileServices implements EchoPlayerService, PlayerProfileQueryService,
        PlayerIdentityMutationService, PlayerSparkMutationService, PlayerAppearanceMutationService,
        PlayerTemplateService, PlayerDialogueMutationService {
    private final Supplier<PlayerProfileRepository> repository;
    private final PlayerMutationLedger ledger;
    private final PlayerProfileTemplate defaultTemplate;
    private final PlayerStructuredLogger logger;
    private final Consumer<PlayerProfileSnapshot> publisher;
    private final PlayerSparkMutation sparkMutation = new PlayerSparkMutation();
    private final PlayerDialogueCallBinder dialogueCalls = new PlayerDialogueCallBinder();

    public ForgePlayerProfileServices(Supplier<PlayerProfileRepository> repository, PlayerMutationLedger ledger,
                                      PlayerProfileTemplate defaultTemplate, PlayerStructuredLogger logger,
                                      Consumer<PlayerProfileSnapshot> publisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.defaultTemplate = Objects.requireNonNull(defaultTemplate, "defaultTemplate");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId) {
        return repository().find(playerId);
    }

    @Override
    public List<PlayerProfileSnapshot> findAll() {
        return repository().findAll();
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> useAccountName(EchoPlayerId playerId,
                                                                     PlayerMutationId mutationId) {
        return mutate(playerId, mutationId, "name:account", snapshot -> {
            PlayerIdentitySnapshot identity = PlayerIdentitySnapshot.account(playerId,
                    snapshot.identity().accountName());
            return replace(snapshot, identity, snapshot.spark(), snapshot.appearance());
        });
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> useCustomName(EchoPlayerId playerId,
                                                                    PlayerMutationId mutationId,
                                                                    PlayerDisplayName displayName) {
        return mutate(playerId, mutationId, "name:custom:" + displayName.value(), snapshot ->
                snapshot.identity().resolvedDisplayName().equals(displayName.value())
                        ? snapshot
                        : replace(snapshot, snapshot.identity().withCustomName(displayName),
                                snapshot.spark(), snapshot.appearance()));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> applyDialogueCall(EchoPlayerId playerId,
                                                                        PlayerMutationId mutationId,
                                                                        InvocationExpressionSyntax call,
                                                                        String source) {
        var displayName = dialogueCalls.bindSetPlayerName(call, source);
        if (!displayName.successful()) {
            return PlayerProfileResult.invalid(displayName.diagnostics());
        }
        return useCustomName(playerId, mutationId, displayName.value().orElseThrow());
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> set(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                          int current) {
        return spark(playerId, mutationId, "spark:set:" + current,
                snapshot -> sparkMutation.set(snapshot.spark().toSpark(), current));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> restore(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                              int amount) {
        return spark(playerId, mutationId, "spark:restore:" + amount,
                snapshot -> sparkMutation.restore(snapshot.spark().toSpark(), amount));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> spend(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                            int amount) {
        return spark(playerId, mutationId, "spark:spend:" + amount,
                snapshot -> sparkMutation.spend(snapshot.spark().toSpark(), amount));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> setMaximum(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                                 int maximum) {
        return spark(playerId, mutationId, "spark:max:" + maximum, snapshot -> sparkMutation.setMaximum(
                snapshot.spark().toSpark(), maximum, MaximumSparkPolicy.CLAMP_CURRENT));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> useAccountSkin(EchoPlayerId playerId,
                                                                     PlayerMutationId mutationId) {
        return mutate(playerId, mutationId, "skin:base", snapshot -> replace(snapshot, snapshot.identity(),
                snapshot.spark(), baseAppearance(snapshot)));
    }

    @Override
    public PlayerProfileResult<PlayerProfileSnapshot> useResourceSkin(EchoPlayerId playerId,
                                                                      PlayerMutationId mutationId,
                                                                      PlayerSkinId skinId,
                                                                      PlayerSkinModel model) {
        return mutate(playerId, mutationId, "skin:base", snapshot -> replace(snapshot, snapshot.identity(),
                snapshot.spark(), baseAppearance(snapshot)));
    }

    @Override
    public PlayerProfileTemplateId defaultTemplateId() {
        return defaultTemplate.id();
    }

    @Override
    public PlayerProfileResult<PlayerProfileTemplateId> reloadTemplates() {
        return PlayerProfileResult.valid(defaultTemplate.id(), List.of());
    }

    private PlayerProfileResult<PlayerProfileSnapshot> spark(EchoPlayerId playerId, PlayerMutationId id,
            String fingerprint, java.util.function.Function<PlayerProfileSnapshot, SparkMutationResult> operation) {
        return mutate(playerId, id, fingerprint, snapshot -> {
            SparkMutationResult result = operation.apply(snapshot);
            if (result.status() != SparkMutationStatus.SUCCESS) {
                return snapshot;
            }
            return replace(snapshot, snapshot.identity(), PlayerSparkSnapshot.from(result.spark()),
                    snapshot.appearance());
        });
    }

    private PlayerProfileResult<PlayerProfileSnapshot> mutate(EchoPlayerId playerId, PlayerMutationId id,
            String fingerprint, java.util.function.Function<PlayerProfileSnapshot, PlayerProfileSnapshot> operation) {
        Optional<PlayerProfileSnapshot> existing = repository().find(playerId);
        if (existing.isEmpty()) {
            return missing(playerId);
        }
        var receipt = ledger.apply(id, fingerprint, () -> "APPLIED");
        if (receipt.conflict()) {
            return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error("PLAYER_MUTATION_CONFLICT",
                    "Mutation ID was already used with different arguments.", playerId.value())));
        }
        PlayerProfileSnapshot current = existing.orElseThrow();
        PlayerProfileSnapshot updated = receipt.replayed() ? current : operation.apply(current);
        PlayerProfileSnapshot snapshot = updated.equals(current) ? current : repository().save(updated);
        logger.debug("profile_mutation", playerId.value(), snapshot.profileRevision().value());
        publisher.accept(snapshot);
        return PlayerProfileResult.valid(snapshot, List.of());
    }

    private PlayerProfileResult<PlayerProfileSnapshot> missing(EchoPlayerId playerId) {
        return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error("PLAYER_PROFILE_MISSING",
                "Profile does not exist.", playerId.value())));
    }

    private PlayerProfileRepository repository() {
        return repository.get();
    }

    private PlayerProfileSnapshot replace(PlayerProfileSnapshot snapshot, PlayerIdentitySnapshot identity,
                                          PlayerSparkSnapshot spark, PlayerAppearanceSnapshot appearance) {
        return new PlayerProfileSnapshot(snapshot.playerId(), identity, spark, appearance, snapshot.templateId(),
                snapshot.templateFingerprint(), snapshot.lifecycle(), snapshot.profileRevision().next(),
                snapshot.repositoryRevision());
    }

    private PlayerAppearanceSnapshot baseAppearance(PlayerProfileSnapshot snapshot) {
        return PlayerBaseSkin.isBase(snapshot.appearance()) ? snapshot.appearance()
                : PlayerBaseSkin.snapshot(snapshot.appearance().appearanceRevision() + 1L);
    }
}
