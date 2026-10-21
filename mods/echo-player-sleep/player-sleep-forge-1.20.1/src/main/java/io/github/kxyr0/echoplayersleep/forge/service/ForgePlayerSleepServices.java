package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.api.claim.PlayerSleepStoryClaim;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepControlMode;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepRecoveryPolicy;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepReturnMode;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmRegistry;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmRequest;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmSnapshot;
import io.github.kxyr0.echoplayersleep.core.scenario.PlayerSleepBedRule;
import io.github.kxyr0.echoplayersleep.core.scenario.PlayerSleepScenarioCatalog;
import io.github.kxyr0.echoplayersleep.core.scenario.PlayerSleepScenarioDefinition;
import io.github.kxyr0.echoplayersleep.core.session.InMemoryPlayerSleepSessionRepository;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepCountLedger;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepSessionFactory;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepSessionTransition;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepStateMachine;
import io.github.kxyr0.echoplayersleep.core.session.VanillaSleepConfirmation;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echoplayersleep.forge.api.arm.PlayerSleepArmService;
import io.github.kxyr0.echoplayersleep.forge.api.catalog.PlayerSleepScenarioCatalogView;
import io.github.kxyr0.echoplayersleep.forge.api.diagnostic.PlayerSleepDiagnosticsView;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import io.github.kxyr0.echoplayersleep.forge.api.session.PlayerSleepSessionQueryService;
import io.github.kxyr0.echoplayersleep.forge.claim.ForgePlayerSleepStoryClaims;
import io.github.kxyr0.echoplayersleep.forge.event.PlayerSleepEventPublisher;
import io.github.kxyr0.echoplayersleep.forge.network.PlayerSleepNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePlayerSleepServices implements PlayerSleepArmService,
        PlayerSleepScenarioCatalogView, PlayerSleepSessionQueryService, PlayerSleepDiagnosticsView {
    private final PlayerSleepScenarioCatalog catalog = new PlayerSleepScenarioCatalog();
    private final PlayerSleepArmRegistry arms = new PlayerSleepArmRegistry();
    private final InMemoryPlayerSleepSessionRepository sessions = new InMemoryPlayerSleepSessionRepository();
    private final PlayerSleepCountLedger sleepCounts = new PlayerSleepCountLedger();
    private final PlayerSleepSessionFactory sessionFactory = new PlayerSleepSessionFactory();
    private final List<PlayerSleepDiagnostic> diagnostics = new ArrayList<>();
    private final InMemoryBedProviderRegistry bedProviders = new InMemoryBedProviderRegistry();
    private final PlayerSleepSessionTransition transition;
    private final PlayerSleepEventPublisher eventPublisher = new PlayerSleepEventPublisher();
    private final ForgePlayerSleepSessionCommands commands;
    private final ForgePlayerSleepStoryClaims claims;
    private MinecraftServer server;

    public ForgePlayerSleepServices() {
        transition = new PlayerSleepSessionTransition(new PlayerSleepStateMachine());
        ForgePlayerSleepLifecyclePublisher lifecyclePublisher =
                new ForgePlayerSleepLifecyclePublisher(this::server, eventPublisher);
        commands = new ForgePlayerSleepSessionCommands(this::server, sessions, transition, arms,
                diagnostics, lifecyclePublisher);
        claims = new ForgePlayerSleepStoryClaims(this::server, sessions, transition, arms,
                diagnostics, eventPublisher);
        catalog.install(new PlayerSleepScenarioDefinition(
                new PlayerSleepScenarioId("echo_player_sleep:test/vanilla_bed_dream"),
                "dream", false, PlayerSleepBedRule.anySupportedBed(), PlayerSleepControlMode.CINEMATIC,
                PlayerSleepReturnMode.WAKE_BESIDE_BED, PlayerSleepRecoveryPolicy.RETURN_TO_BED, 0L));
    }

    public void useServer(MinecraftServer server) { this.server = Objects.requireNonNull(server, "server"); }

    public boolean owns(MinecraftServer server) { return this.server == server; }

    public PlayerSleepServiceBridge bridge() {
        return new PlayerSleepServiceBridge(this, this, commands, this, bedProviders, claims,
                commands, this, commands);
    }

    public InMemoryBedProviderRegistry bedProviders() {
        return bedProviders;
    }

    public PlayerSleepSessionQueryService query() { return this; }

    public Optional<PlayerSleepArmSnapshot> activeArm(PlayerRef player) {
        return arms.activeArm(player);
    }

    public Optional<PlayerSleepStoryClaim> storyClaim(PlayerSleepSessionSnapshot snapshot) {
        return claims.claimFor(snapshot.sessionId());
    }

    public Optional<PlayerSleepEventSnapshot> event(PlayerSleepEventId eventId) {
        return eventPublisher.event(eventId);
    }

    public Optional<ServerPlayer> player(PlayerRef player) {
        return server == null ? Optional.empty()
                : Optional.ofNullable(server.getPlayerList().getPlayer(player.uuid()));
    }

    public List<PlayerSleepEventSnapshot> events() {
        return eventPublisher.events();
    }

    @Override
    public PlayerSleepResult arm(PlayerRef player, PlayerSleepScenarioId scenarioId,
                                 PlayerSleepOwnerId owner, PlayerSleepCommandId commandId) {
        if (catalog.find(scenarioId).isEmpty()) {
            return fail(PlayerSleepOutcome.SKIPPED, PlayerSleepFailure.PLAYER_SLEEP_SCENARIO_NOT_FOUND,
                    scenarioId.value(), "Sleep scenario is not loaded.");
        }
        var decision = arms.arm(new PlayerSleepArmRequest(player, scenarioId, owner, commandId, tick()));
        decision.diagnostics().forEach(diagnostics::add);
        return decision.accepted() ? PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED)
                : new PlayerSleepResult(PlayerSleepOutcome.INTERNAL_FAILURE, decision.diagnostics());
    }

    @Override
    public boolean disarm(PlayerRef player) {
        return arms.disarm(player);
    }

    @Override
    public boolean isArmed(PlayerRef player) {
        return arms.activeArm(player).isPresent();
    }

    @Override
    public boolean isArmed(PlayerRef player, PlayerSleepScenarioId scenarioId) {
        return arms.isArmed(player, scenarioId);
    }

    public void confirmVanillaSleep(VanillaSleepConfirmation confirmation) {
        long sleepCount = recordSleepCount(confirmation.player());
        Optional<PlayerSleepArmSnapshot> arm = arms.activeArm(confirmation.player());
        if (arm.isEmpty()) {
            return;
        }
        PlayerSleepScenarioDefinition scenario = catalog.find(arm.orElseThrow().scenarioId()).orElse(null);
        if (scenario == null) {
            record(PlayerSleepFailure.PLAYER_SLEEP_SCENARIO_NOT_FOUND, arm.orElseThrow().scenarioId().value());
            return;
        }
        PlayerSleepSessionSnapshot confirmed = sessionFactory.create(arm.orElseThrow(), scenario,
                confirmation, sleepCount);
        PlayerSleepSessionSnapshot waiting = transition.move(confirmed, PlayerSleepStatus.WAITING_FOR_STORY_CLAIM);
        if (sessions.saveNew(waiting)) {
            publish(PlayerSleepLifecyclePhase.STORY_CLAIM_WAITING, waiting);
            eventPublisher.publish(PlayerSleepEventKind.CONFIRMED, PlayerSleepLifecyclePhase.VANILLA_CONFIRMED,
                    waiting, confirmation.serverTick());
        }
    }

    public void vanillaConfirmationTimedOut(PlayerRef player) {
        record(PlayerSleepFailure.PLAYER_SLEEP_VANILLA_CONFIRMATION_TIMEOUT, player.uuid().toString());
    }

    @Override
    public long sleepCount(PlayerRef player) {
        if (server == null) {
            return sleepCounts.count(player);
        }
        return ForgePlayerSleepCountSavedData.get(server).count(player);
    }

    public long resetSleepCount(PlayerRef player) {
        if (server == null) {
            return sleepCounts.reset(player);
        }
        return ForgePlayerSleepCountSavedData.get(server).reset(player);
    }

    public PlayerSleepClearReport clearForTesting(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        Optional<PlayerSleepSessionSnapshot> active = sessions.activeFor(player);
        Optional<PlayerSleepArmSnapshot> activeArm = arms.activeArm(player);
        PlayerSleepRevision resetRevision = active.map(PlayerSleepSessionSnapshot::revision)
                .map(PlayerSleepRevision::next)
                .orElse(PlayerSleepRevision.initial());
        boolean returnedToBed = active.map(this::returnToStoredBed).orElse(false);
        if (active.isPresent()) {
            commands.cancel(player, new PlayerSleepCommandId("test-clear:" + tick() + ":" + player.uuid()));
        }
        boolean disarmed = arms.disarm(player);
        int claimsRemoved = claims.clearFor(player);
        int sessionsRemoved = sessions.clearFor(player);
        int eventsRemoved = eventPublisher.clearFor(player);
        long previousSleepCount = resetSleepCount(player);
        int diagnosticsRemoved = diagnostics.size();
        diagnostics.clear();
        boolean clientReset = resetClientOverlay(player, resetRevision);
        return new PlayerSleepClearReport(active.isPresent(), activeArm.isPresent(), disarmed, sessionsRemoved,
                claimsRemoved, eventsRemoved, previousSleepCount, diagnosticsRemoved, clientReset, returnedToBed);
    }

    @Override
    public List<PlayerSleepScenarioId> scenarioIds() {
        return catalog.all().stream().map(PlayerSleepScenarioDefinition::scenarioId).toList();
    }

    @Override
    public boolean contains(PlayerSleepScenarioId scenarioId) {
        return catalog.find(scenarioId).isPresent();
    }

    @Override
    public Optional<PlayerSleepSessionSnapshot> active(PlayerRef player) {
        return sessions.activeFor(player);
    }

    @Override
    public List<PlayerSleepSessionSnapshot> activeSessions() {
        return sessions.activeSessions();
    }

    @Override
    public List<PlayerSleepDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    private MinecraftServer server() {
        return server;
    }

    private PlayerSleepResult fail(PlayerSleepOutcome outcome, PlayerSleepFailure code,
                                   String subject, String message) {
        PlayerSleepDiagnostic diagnostic = PlayerSleepDiagnostic.error(code, subject, message);
        diagnostics.add(diagnostic);
        return PlayerSleepResult.failure(outcome, diagnostic);
    }

    private void record(PlayerSleepFailure code, String subject) {
        diagnostics.add(PlayerSleepDiagnostic.error(code, subject, code.name()));
    }

    private void publish(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        eventPublisher.publishLifecycle(phase, snapshot);
    }

    private long recordSleepCount(PlayerRef player) {
        if (server == null) {
            return sleepCounts.recordConfirmedSleep(player);
        }
        return ForgePlayerSleepCountSavedData.get(server).record(player);
    }

    private boolean resetClientOverlay(PlayerRef player, PlayerSleepRevision revision) {
        Optional<ServerPlayer> target = player(player);
        target.ifPresent(serverPlayer -> PlayerSleepNetwork.openEyes(serverPlayer, revision, Ticks.of(1L),
                false, true));
        return target.isPresent();
    }

    private boolean returnToStoredBed(PlayerSleepSessionSnapshot snapshot) {
        if (snapshot.ownsBedPose()) {
            return false;
        }
        ForgePlayerSleepPlacement placement = new ForgePlayerSleepPlacement(this::server);
        return player(snapshot.player()).map(player -> placement.returnToBed(player, snapshot)).orElse(false);
    }

    private long tick() {
        return server == null ? 0L : server.getTickCount();
    }

    public record PlayerSleepClearReport(
            boolean hadActiveSession,
            boolean hadActiveArm,
            boolean disarmed,
            int sessionsRemoved,
            int claimsRemoved,
            int eventsRemoved,
            long previousSleepCount,
            int diagnosticsRemoved,
            boolean clientOverlayReset,
            boolean returnedToBed) {
    }
}
