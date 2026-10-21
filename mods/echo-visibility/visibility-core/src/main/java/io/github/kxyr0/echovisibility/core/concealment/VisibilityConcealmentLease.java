package io.github.kxyr0.echovisibility.core.concealment;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import java.util.Objects;

public final class VisibilityConcealmentLease {
    private final VisibilityConcealmentLeaseId id;
    private final VisibilityWatchId watchId;
    private final VisibilityWatchGeneration generation;
    private final VisibilityDefinitionRevision targetRevision;
    private final String npcId;
    private final long createdSequence;
    private final long deadlineTick;
    private VisibilityConcealmentStatus status = VisibilityConcealmentStatus.PENDING;

    public VisibilityConcealmentLease(
            VisibilityConcealmentLeaseId id,
            VisibilityWatchId watchId,
            VisibilityWatchGeneration generation,
            VisibilityDefinitionRevision targetRevision,
            String npcId,
            long createdSequence,
            long deadlineTick) {
        this.id = Objects.requireNonNull(id, "id");
        this.watchId = Objects.requireNonNull(watchId, "watchId");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.targetRevision = Objects.requireNonNull(targetRevision, "targetRevision");
        this.npcId = requireNpcId(npcId);
        if (createdSequence <= 0L || deadlineTick < 0L) {
            throw new IllegalArgumentException("Concealment sequence or deadline is invalid.");
        }
        this.createdSequence = createdSequence;
        this.deadlineTick = deadlineTick;
    }

    public boolean claim() {
        if (status != VisibilityConcealmentStatus.PENDING) {
            return false;
        }
        status = VisibilityConcealmentStatus.CLAIMED;
        return true;
    }

    public boolean commit() {
        if (status != VisibilityConcealmentStatus.PENDING
                && status != VisibilityConcealmentStatus.CLAIMED) {
            return false;
        }
        status = VisibilityConcealmentStatus.COMMITTED;
        return true;
    }

    public boolean reject() {
        if (terminal()) {
            return false;
        }
        status = VisibilityConcealmentStatus.REJECTED;
        return true;
    }

    public boolean expire(long currentTick) {
        if (status != VisibilityConcealmentStatus.PENDING || currentTick <= deadlineTick) {
            return false;
        }
        status = VisibilityConcealmentStatus.TIMED_OUT;
        return true;
    }

    public boolean hidesNpc() {
        return status == VisibilityConcealmentStatus.PENDING
                || status == VisibilityConcealmentStatus.CLAIMED;
    }

    public boolean terminal() {
        return status == VisibilityConcealmentStatus.COMMITTED
                || status == VisibilityConcealmentStatus.REJECTED
                || status == VisibilityConcealmentStatus.TIMED_OUT;
    }

    public Snapshot snapshot() {
        return new Snapshot(id, watchId, generation, targetRevision, npcId,
                createdSequence, deadlineTick, status);
    }

    private static String requireNpcId(String value) {
        String checked = Objects.requireNonNull(value, "npcId").trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("NPC id must be namespace:path.");
        }
        return checked;
    }

    public record Snapshot(
            VisibilityConcealmentLeaseId id,
            VisibilityWatchId watchId,
            VisibilityWatchGeneration generation,
            VisibilityDefinitionRevision targetRevision,
            String npcId,
            long createdSequence,
            long deadlineTick,
            VisibilityConcealmentStatus status) {
    }
}
