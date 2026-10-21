package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VisibilityArmRegistry {
    public static final int DEFAULT_MAXIMUM_ACTIVE_WATCHES = 32;
    private final int maximumActiveWatches;
    private final Map<VisibilityPlayerRef, Map<VisibilityWatchId, ArmedVisibilityWatch>> watches = new HashMap<>();
    private final Map<ArmKey, Long> generations = new HashMap<>();

    public VisibilityArmRegistry() {
        this(DEFAULT_MAXIMUM_ACTIVE_WATCHES);
    }

    public VisibilityArmRegistry(int maximumActiveWatches) {
        if (maximumActiveWatches < 1 || maximumActiveWatches > 256) {
            throw new IllegalArgumentException("Maximum active watches must be within 1..256.");
        }
        this.maximumActiveWatches = maximumActiveWatches;
    }

    public VisibilityArmResult arm(
            VisibilityPlayerRef player,
            VisibilityWatchDefinition definition,
            String ownerLineage,
            long serverTick,
            long connectionGeneration) {
        Map<VisibilityWatchId, ArmedVisibilityWatch> playerWatches =
                watches.computeIfAbsent(player, ignored -> new HashMap<>());
        ArmedVisibilityWatch existing = playerWatches.get(definition.id());
        if (existing != null) {
            return result(VisibilityArmResult.Status.ALREADY_ARMED, existing,
                    VisibilityDiagnosticCode.VISIBILITY_WATCH_ALREADY_ARMED, "Watch is already armed.");
        }
        if (playerWatches.size() >= maximumActiveWatches) {
            return result(VisibilityArmResult.Status.LIMIT_REACHED, null,
                    VisibilityDiagnosticCode.VISIBILITY_ACTIVE_WATCH_LIMIT, "Active watch limit reached.");
        }
        ArmKey key = new ArmKey(player, definition.id());
        long generationValue = generations.merge(key, 1L, Long::sum);
        ArmedVisibilityWatch armed = new ArmedVisibilityWatch(player, definition,
                new VisibilityWatchGeneration(generationValue), ownerLineage, serverTick, connectionGeneration);
        playerWatches.put(definition.id(), armed);
        return new VisibilityArmResult(VisibilityArmResult.Status.ARMED,
                Optional.of(armed.snapshot()), Optional.empty());
    }

    public VisibilityArmResult disarm(VisibilityPlayerRef player, VisibilityWatchId watchId) {
        Map<VisibilityWatchId, ArmedVisibilityWatch> playerWatches = watches.get(player);
        ArmedVisibilityWatch removed = playerWatches == null ? null : playerWatches.remove(watchId);
        if (removed == null) {
            return result(VisibilityArmResult.Status.NOT_ARMED, null,
                    VisibilityDiagnosticCode.VISIBILITY_WATCH_NOT_ARMED, "Watch is not armed.");
        }
        if (playerWatches.isEmpty()) {
            watches.remove(player);
        }
        return new VisibilityArmResult(VisibilityArmResult.Status.DISARMED,
                Optional.of(removed.snapshot()), Optional.empty());
    }

    public Optional<ArmedVisibilityWatch> find(VisibilityPlayerRef player, VisibilityWatchId watchId) {
        return Optional.ofNullable(watches.get(player)).map(values -> values.get(watchId));
    }

    public List<VisibilityArmSnapshot> active(VisibilityPlayerRef player) {
        return Optional.ofNullable(watches.get(player)).stream().flatMap(values -> values.values().stream())
                .map(ArmedVisibilityWatch::snapshot).sorted((left, right) ->
                left.definition().id().compareTo(right.definition().id())).toList();
    }

    public List<VisibilityArmSnapshot> all() {
        return watches.values().stream().flatMap(values -> values.values().stream())
                .map(ArmedVisibilityWatch::snapshot).toList();
    }

    public List<VisibilityGenerationSnapshot> generations() {
        return generations.entrySet().stream().map(entry -> new VisibilityGenerationSnapshot(
                entry.getKey().player(), entry.getKey().watchId(),
                new VisibilityWatchGeneration(entry.getValue()))).toList();
    }

    public void restoreGeneration(VisibilityGenerationSnapshot snapshot) {
        ArmKey key = new ArmKey(snapshot.player(), snapshot.watchId());
        generations.merge(key, snapshot.generation().value(), Math::max);
    }

    public void restore(VisibilityArmRecovery recovery) {
        Map<VisibilityWatchId, ArmedVisibilityWatch> playerWatches =
                watches.computeIfAbsent(recovery.player(), ignored -> new HashMap<>());
        if (playerWatches.size() >= maximumActiveWatches
                || playerWatches.containsKey(recovery.definition().id())) {
            return;
        }
        playerWatches.put(recovery.definition().id(), new ArmedVisibilityWatch(recovery.player(),
                recovery.definition(), recovery.generation(), recovery.ownerLineage(), recovery.createdTick(),
                recovery.connectionGeneration()));
        ArmKey key = new ArmKey(recovery.player(), recovery.definition().id());
        generations.merge(key, recovery.generation().value(), Math::max);
    }

    private static VisibilityArmResult result(
            VisibilityArmResult.Status status,
            ArmedVisibilityWatch arm,
            VisibilityDiagnosticCode code,
            String message) {
        return new VisibilityArmResult(status, Optional.ofNullable(arm).map(ArmedVisibilityWatch::snapshot),
                Optional.of(VisibilityDiagnostic.of(code, message)));
    }

    private record ArmKey(VisibilityPlayerRef player, VisibilityWatchId watchId) {
    }
}
