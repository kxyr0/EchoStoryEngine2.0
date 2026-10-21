package io.github.kxyr0.echoteleportmagic.runtime;

import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.api.TeleportState;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.kxyr0.echoteleportmagic.runtime.TeleportPort.ReceiptStage;

public final class TeleportCoordinator {
    private final TeleportPort port;
    private final Map<TeleportExecutionId, TeleportExecution> active = new LinkedHashMap<>();
    private final java.util.Set<TeleportExecutionId> committing = new java.util.HashSet<>();
    private final java.util.Set<TeleportExecutionId> cancelAfterCommit = new java.util.HashSet<>();
    private final Map<TeleportExecutionId, Long> completionTicks = new LinkedHashMap<>();

    public TeleportCoordinator(TeleportPort port) { this.port = port; }

    public TeleportExecution start(TeleportExecution request) {
        if (active.containsKey(request.id())) return active.get(request.id());
        ReceiptStage recovered = port.receipt(request.id());
        if (recovered == ReceiptStage.AFTER_COMMIT || recovered == ReceiptStage.COMPLETED) {
            return request.state(TeleportState.COMPLETED, true);
        }
        if (recovered == ReceiptStage.COMMIT_INTENT) {
            throw new IllegalStateException("TELEPORT_RECOVERY_AMBIGUOUS: automatic replay forbidden");
        }
        if (recovered == ReceiptStage.CANCELLED || recovered == ReceiptStage.FAILED) {
            throw new IllegalStateException("TELEPORT_EXECUTION_TERMINAL");
        }
        if (active.values().stream().anyMatch(e -> e.subject().equals(request.subject()))) {
            throw new IllegalStateException("TELEPORT_SUBJECT_BUSY");
        }
        port.prepare(request);
        TeleportExecution started = request.state(TeleportState.DEPARTING, false);
        port.receipt(started, ReceiptStage.BEFORE_COMMIT);
        active.put(started.id(), started);
        completionTicks.put(started.id(), started.startTick() + TeleportTimeline.COMPLETE_TICK);
        try { port.visualStart(started); }
        catch (RuntimeException failure) { terminate(started.id(), TeleportState.FAILED); throw failure; }
        return started;
    }
    public void tick(long tick) {
        for (TeleportExecution execution : List.copyOf(active.values())) {
            try { advance(execution, tick); }
            catch (RuntimeException failure) { terminate(execution.id(), TeleportState.FAILED); }
        }
    }
    private void advance(TeleportExecution e, long tick) {
        if (!e.committed() && !port.alive(e)) { terminate(e.id(), TeleportState.CANCELLED); return; }
        long elapsed = tick - e.startTick();
        if (!e.committed() && elapsed < TeleportTimeline.COMMIT_TICK) {
            e = e.anchor(port.anchor(e));
            e = e.state(elapsed >= TeleportTimeline.COMMIT_TICK - 4 ? TeleportState.CONCEALED : TeleportState.DEPARTING, false);
            active.put(e.id(), e);
        }
        if (!e.committed() && elapsed >= TeleportTimeline.COMMIT_TICK) e = commit(e, tick);
        if (e.committed() && cancelAfterCommit.remove(e.id())) { terminate(e.id(), TeleportState.CANCELLED); return; }
        if (elapsed >= TeleportTimeline.COMPLETE_TICK && !e.committed()) {
            throw new IllegalStateException("TELEPORT_COMMIT_TIMEOUT");
        }
        if (tick >= completionTicks.get(e.id())) {
            TeleportExecution completed = e.state(TeleportState.COMPLETED, e.committed());
            port.receipt(completed, ReceiptStage.COMPLETED);
            active.remove(e.id());
            completionTicks.remove(e.id());
            port.finished(completed);
        } else if (e.committed()) {
            active.put(e.id(), e.state(elapsed < TeleportTimeline.COMMIT_TICK + 17 ? TeleportState.ARRIVING : TeleportState.RESIDUAL, true));
        }
    }
    private TeleportExecution commit(TeleportExecution e, long tick) {
        if (committing.add(e.id())) {
            e = e.anchor(port.anchor(e));
            port.receipt(e, ReceiptStage.COMMIT_INTENT);
            active.put(e.id(), e);
        }
        if (!e.preview() && !port.commit(e)) return e;
        committing.remove(e.id());
        e = e.state(TeleportState.COMMITTED, true);
        active.put(e.id(), e);
        completionTicks.put(e.id(), tick + TeleportTimeline.COMPLETE_TICK - TeleportTimeline.COMMIT_TICK);
        port.receipt(e, ReceiptStage.AFTER_COMMIT);
        port.visualCommit(e);
        return e;
    }
    public void cancel(TeleportExecutionId id) { terminate(id, TeleportState.CANCELLED); }
    public void cancelOwned(String owner) {
        for (TeleportExecution e : List.copyOf(active.values())) if (e.owner().equals(owner)) cancel(e.id());
    }
    private void terminate(TeleportExecutionId id, TeleportState state) {
        if (state == TeleportState.CANCELLED && committing.contains(id)) { cancelAfterCommit.add(id); return; }
        TeleportExecution e = active.remove(id);
        committing.remove(id);
        cancelAfterCommit.remove(id);
        completionTicks.remove(id);
        if (e == null) return;
        TeleportExecution terminal = e.state(state, e.committed());
        // Preserve the commit receipt even when presentation fails after gameplay commit.
        port.receipt(terminal, e.committed() ? ReceiptStage.AFTER_COMMIT
                : state == TeleportState.FAILED ? ReceiptStage.FAILED : ReceiptStage.CANCELLED);
        port.finished(terminal);
    }
    public List<TeleportExecution> active() { return List.copyOf(active.values()); }
    public Optional<TeleportExecution> find(TeleportExecutionId id) { return Optional.ofNullable(active.get(id)); }
}

