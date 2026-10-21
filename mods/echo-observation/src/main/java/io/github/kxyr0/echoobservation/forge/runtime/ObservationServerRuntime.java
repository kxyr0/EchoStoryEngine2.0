package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoobservation.api.action.ObservationActionRegistry;
import io.github.kxyr0.echoobservation.forge.domain.ObservationDomainOutbox;
import io.github.kxyr0.echoobservation.forge.persistence.ObservationDurableStore;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.lifecycle.ObservationCompletedEvent;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalog;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogCandidate;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogSnapshot;
import io.github.kxyr0.echoobservation.core.focus.ObservationFocusSession;
import io.github.kxyr0.echoobservation.core.focus.ObservationFocusSessions;
import io.github.kxyr0.echoobservation.core.progress.ObservationCompletionFlow;
import io.github.kxyr0.echoobservation.core.progress.ObservationPlayerProgress;
import io.github.kxyr0.echoobservation.forge.network.ObservationNetwork;
import io.github.kxyr0.echoobservation.forge.network.ObservationPackets;
import io.github.kxyr0.echoobservation.forge.persistence.ObservationProgressSavedData;
import io.github.kxyr0.echoobservation.forge.persistence.ObservationProgressMigration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ObservationServerRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("echo_observation");
    private static final ObservationServerRuntime INSTANCE = new ObservationServerRuntime();
    private final ObservationCatalog catalog = new ObservationCatalog();
    private final ObservationFocusSessions sessions = new ObservationFocusSessions();
    private final ObservationActionRegistry actions = new ObservationActionRegistry();
    private final ObservationCompletionFlow completion = new ObservationCompletionFlow(actions);
    private final ObservationEligibility eligibility = new ObservationEligibility();
    private final ObservationService service = new ForgeObservationService(this);
    private final ObservationProgressMigration migration = new ObservationProgressMigration();
    private final Map<UUID, ObservationDefinition> pinned = new LinkedHashMap<>();
    private MinecraftServer server;
    private ObservationProgressSavedData savedData;
    private long snapshotSequence;

    private ObservationServerRuntime() {
    }

    public static ObservationServerRuntime instance() { return INSTANCE; }
    public ObservationService service() { return service; }
    public ObservationCatalogSnapshot catalog() { return catalog.snapshot(); }
    public int focusSessionCount() { return sessions.size(); }

    public boolean reset(UUID playerId, ObservationId observationId) {
        requireStarted(); sessions.stop(playerId); pinned.remove(playerId);
        boolean changed = progress(playerId).resetCompletion(observationId);
        if (changed) {
            durable(); ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(playerId);
            if (player != null) sync(player);
        }
        return changed;
    }

    public void start(MinecraftServer value) {
        server = value;
        savedData = ObservationProgressSavedData.get(value);
        migration.apply(catalog.snapshot(), savedData);
        actions.clear();
        ObservationActionExecutors.register(actions, value);
        durable();
        ObservationDomainOutbox.publish(value, savedData);
        value.getPlayerList().getPlayers().forEach(this::sync);
    }

    public void stop() {
        pinned.clear();
        server = null;
        savedData = null;
    }

    public void install(ObservationCatalogCandidate candidate) {
        candidate.diagnostics().forEach(value -> LOGGER.warn("{}: {}", value.code(), value.message()));
        if (catalog.install(candidate) && server != null) {
            migration.apply(catalog.snapshot(), savedData);
            server.getPlayerList().getPlayers().forEach(this::sync);
        }
    }

    public ObservationPlayerProgress progress(UUID playerId) {
        requireStarted();
        return savedData.book().player(playerId);
    }

    public void mutate(UUID playerId, Function<ObservationPlayerProgress, Boolean> mutation) {
        ObservationDurableStore.mutate(server, savedData, () -> mutation.apply(progress(playerId)));
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) sync(player);
        }
    }

    public void focus(ServerPlayer player, ObservationPackets.ServerboundFocus packet) {
        ObservationId id;
        try { id = ObservationId.parse(packet.observationId()); }
        catch (IllegalArgumentException exception) { ObservationNetwork.reject(player, packet.observationId(), "OBS_FOCUS_TARGET_INVALID"); return; }
        long tick = player.serverLevel().getGameTime();
        if (packet.kind() == ObservationPackets.FocusKind.STOP) {
            sessions.stop(player.getUUID()); pinned.remove(player.getUUID());
            ObservationNetwork.focusState(player, ObservationPackets.FocusStatus.STOPPED, id, 0, "stopped"); return;
        }
        if (packet.kind() == ObservationPackets.FocusKind.HEARTBEAT) {
            if (!sessions.heartbeat(player.getUUID(), id, packet.definitionRevision(), tick)) {
                ObservationNetwork.reject(player, id.toString(), "OBS_FOCUS_SESSION_STALE");
            }
            return;
        }
        ObservationDefinition definition = catalog.snapshot().observation(id).orElse(null);
        if (definition == null || definition.revision() != packet.definitionRevision()
                || !eligibility.focusable(player, definition, progress(player.getUUID()))) {
            ObservationNetwork.reject(player, id.toString(), "OBS_FOCUS_SERVER_REJECTED"); return;
        }
        sessions.start(player.getUUID(), definition, tick);
        pinned.put(player.getUUID(), definition);
        ObservationNetwork.focusState(player, ObservationPackets.FocusStatus.ACCEPTED, id, definition.focus().ticks(), "accepted");
    }

    public void tick() {
        if (server == null) return;
        long tick = server.overworld().getGameTime();
        for (ObservationFocusSession session : sessions.completable(tick)) complete(session);
    }

    public void sync(ServerPlayer player) {
        ObservationPlayerProgress state = progress(player.getUUID());
        var packet = new ObservationPackets.ClientboundSnapshot(++snapshotSequence, catalog.snapshot().revision(),
                java.util.List.copyOf(catalog.snapshot().resources()),
                state.activeResources().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()),
                state.disabledObservations().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()),
                state.completions().keySet().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()));
        ObservationNetwork.send(player, packet);
    }

    public void reload(ServerPlayer player, ObservationPackets.ServerboundReload packet) {
        if (server == null || server.isDedicatedServer()) {
            reloadResult(player, packet.observationId(), false, "OBS_AUTHORING_DISABLED"); return;
        }
        ObservationId id;
        try { id = ObservationId.parse(packet.observationId()); }
        catch (IllegalArgumentException exception) { reloadResult(player, packet.observationId(), false,
                "OBSERVATION_NOT_FOUND"); return; }
        long previous = catalog.snapshot().observation(id).map(ObservationDefinition::revision).orElse(-1L);
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored, failure) ->
                server.execute(() -> {
                    long current = catalog.snapshot().observation(id).map(ObservationDefinition::revision).orElse(-1L);
                    boolean success = failure == null && current >= 0L && current != previous;
                    reloadResult(player, id.toString(), success,
                            success ? "PLACED" : "OBS_AUTHORING_RELOAD_FAILED");
                }));
    }

    private void complete(ObservationFocusSession session) {
        ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
        ObservationDefinition definition = pinned.remove(session.playerId());
        if (player == null || definition == null || !eligibility.focusable(player, definition, progress(session.playerId()))) {
            return;
        }
        ObservationPlayerProgress playerProgress = progress(session.playerId());
        var marked = ObservationDomainOutbox.mark(server, savedData, session.playerId(), definition, completion);
        ObservationDomainOutbox.publish(server, savedData);
        marked.completionId().ifPresent(id -> MinecraftForge.EVENT_BUS.post(new ObservationCompletedEvent(
                session.playerId(), definition.id(), id, definition.source().sourceId())));
        definition.text().ifPresent(text -> ObservationNetwork.send(player, new ObservationPackets.ClientboundText(
                definition.id().toString(), text, System.currentTimeMillis() + 5000L)));
        var result = completion.execute(session.playerId(), definition, marked.completionId().orElseThrow(),
                playerProgress, marked.status()
                        == io.github.kxyr0.echoobservation.core.progress.ObservationCompletionResult.Status.ALREADY_COMPLETED);
        durable();
        ObservationNetwork.focusState(player, ObservationPackets.FocusStatus.COMPLETED, definition.id(),
                definition.focus().ticks(), result.status().name());
        sync(player);
    }

    private void reloadResult(ServerPlayer player, String id, boolean success, String message) {
        ObservationNetwork.send(player, new ObservationPackets.ClientboundReload(id, success,
                catalog.snapshot().revision(), message));
    }

    private void durable() {
        savedData.setDirty();
        ObservationDurableStore.commit(server, savedData);
    }
    private void requireStarted() {
        if (savedData == null) throw new IllegalStateException("Observation server runtime is not started.");
    }

}
