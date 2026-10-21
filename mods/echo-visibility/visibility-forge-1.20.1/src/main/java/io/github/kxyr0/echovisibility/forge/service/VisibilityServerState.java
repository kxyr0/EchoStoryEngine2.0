package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityEventId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.core.arm.ArmedVisibilityWatch;
import io.github.kxyr0.echovisibility.core.arm.VisibilityArmRegistry;
import io.github.kxyr0.echovisibility.core.catalog.VisibilityWatchCatalogState;
import io.github.kxyr0.echovisibility.core.concealment.VisibilityConcealmentLease;
import io.github.kxyr0.echovisibility.core.concealment.VisibilityConcealmentLedger;
import io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolution;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class VisibilityServerState {
    private final MinecraftServer server;
    private final VisibilityWatchCatalogState catalog = new VisibilityWatchCatalogState();
    private final VisibilityArmRegistry arms = new VisibilityArmRegistry();
    private final ForgeVisibilityTargetResolver resolver = new ForgeVisibilityTargetResolver();
    private final VisibilityDiagnosticLog diagnostics = new VisibilityDiagnosticLog();
    private final Map<VisibilityPlayerRef, Map<VisibilityWatchId, VisibilityTargetResolution>> resolutions =
            new HashMap<>();
    private final Map<VisibilityPlayerRef, VisibilityConcealmentLedger> concealments = new HashMap<>();
    private final Map<UUID, Long> connectionGenerations = new HashMap<>();
    private final Set<VisibilityEventId> emittedEvents = new HashSet<>();
    private Runnable persistenceDirty = () -> { };

    public VisibilityServerState(MinecraftServer server) {
        this.server = server;
    }

    public long connect(ServerPlayer player) {
        VisibilityPlayerRef playerRef = player(player);
        long activeConnection = arms.active(playerRef).stream()
                .mapToLong(value -> value.connectionGeneration()).max().orElse(0L);
        long previous = Math.max(connectionGenerations.getOrDefault(player.getUUID(), 0L), activeConnection);
        long connection = previous + 1L;
        connectionGenerations.put(player.getUUID(), connection);
        for (var snapshot : arms.active(playerRef)) {
            arms.find(playerRef, snapshot.definition().id()).ifPresent(arm -> arm.reconnect(connection));
        }
        concealments.computeIfAbsent(playerRef, ignored -> new VisibilityConcealmentLedger()).rejectActive();
        markDirty();
        return connection;
    }

    public void persistenceDirty(Runnable value) {
        persistenceDirty = java.util.Objects.requireNonNull(value, "persistenceDirty");
    }

    public void markDirty() {
        persistenceDirty.run();
    }

    public long connection(ServerPlayer player) {
        return connectionGenerations.computeIfAbsent(player.getUUID(), ignored -> 1L);
    }

    public List<VisibilityConcealmentLease.Snapshot> disconnect(ServerPlayer player) {
        VisibilityPlayerRef playerRef = player(player);
        resolutions.remove(playerRef);
        return Optional.ofNullable(concealments.get(playerRef))
                .map(VisibilityConcealmentLedger::rejectActive).orElseGet(List::of);
    }

    public VisibilityTargetResolution resolve(ServerPlayer player, ArmedVisibilityWatch arm) {
        VisibilityTargetResolution resolution = resolver.resolve(player, arm.definition());
        resolutions.computeIfAbsent(player(player), ignored -> new HashMap<>())
                .put(arm.definition().id(), resolution);
        resolution.diagnostic().ifPresent(diagnostics::add);
        return resolution;
    }

    public Optional<VisibilityTargetResolution> resolution(
            VisibilityPlayerRef player,
            VisibilityWatchId watchId) {
        return Optional.ofNullable(resolutions.get(player)).map(values -> values.get(watchId));
    }

    public Optional<VisibilityConcealmentLease> createLease(
            VisibilityPlayerRef player,
            VisibilityReport report) {
        if (report.concealmentLeaseId().isEmpty()) {
            return Optional.empty();
        }
        String npcId = ((VisibilityNpcTarget) arms.find(player, report.watchId()).orElseThrow()
                .definition().target()).npcId();
        VisibilityConcealmentLease lease = new VisibilityConcealmentLease(
                report.concealmentLeaseId().orElseThrow(), report.watchId(), report.generation(),
                report.targetRevision(), npcId, report.reportSequence(), server.getTickCount() + 10L);
        VisibilityConcealmentLedger ledger = concealments.computeIfAbsent(
                player, ignored -> new VisibilityConcealmentLedger());
        return ledger.add(lease) ? Optional.of(lease) : Optional.empty();
    }

    public Optional<VisibilityConcealmentLease> lease(
            VisibilityPlayerRef player,
            VisibilityConcealmentLeaseId leaseId) {
        return Optional.ofNullable(concealments.get(player)).flatMap(ledger -> ledger.find(leaseId));
    }

    public List<VisibilityConcealmentLease.Snapshot> leases(VisibilityPlayerRef player) {
        return Optional.ofNullable(concealments.get(player))
                .map(VisibilityConcealmentLedger::snapshots).orElseGet(List::of);
    }

    public List<VisibilityConcealmentLease.Snapshot> expire(VisibilityPlayerRef player, long tick) {
        return Optional.ofNullable(concealments.get(player))
                .map(ledger -> ledger.expire(tick)).orElseGet(List::of);
    }

    public boolean firstEvent(VisibilityEventId eventId) {
        return emittedEvents.add(eventId);
    }

    public MinecraftServer server() { return server; }
    public VisibilityWatchCatalogState catalog() { return catalog; }
    public VisibilityArmRegistry arms() { return arms; }
    public VisibilityDiagnosticLog diagnostics() { return diagnostics; }

    public static VisibilityPlayerRef player(ServerPlayer player) {
        return new VisibilityPlayerRef(player.getUUID());
    }
}
