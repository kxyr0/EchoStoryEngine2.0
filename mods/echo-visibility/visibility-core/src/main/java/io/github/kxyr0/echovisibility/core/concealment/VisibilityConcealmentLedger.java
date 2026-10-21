package io.github.kxyr0.echovisibility.core.concealment;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VisibilityConcealmentLedger {
    private final Map<VisibilityConcealmentLeaseId, VisibilityConcealmentLease> leases = new LinkedHashMap<>();

    public boolean add(VisibilityConcealmentLease lease) {
        return leases.putIfAbsent(lease.snapshot().id(), lease) == null;
    }

    public Optional<VisibilityConcealmentLease> find(VisibilityConcealmentLeaseId id) {
        return Optional.ofNullable(leases.get(id));
    }

    public boolean hidesNpc(String npcId) {
        return leases.values().stream().anyMatch(lease ->
                lease.snapshot().npcId().equals(npcId) && lease.hidesNpc());
    }

    public List<VisibilityConcealmentLease.Snapshot> snapshots() {
        return leases.values().stream().map(VisibilityConcealmentLease::snapshot).toList();
    }

    public List<VisibilityConcealmentLease.Snapshot> expire(long currentTick) {
        List<VisibilityConcealmentLease.Snapshot> expired = leases.values().stream()
                .filter(lease -> lease.expire(currentTick)).map(VisibilityConcealmentLease::snapshot).toList();
        removeTerminal(expired.stream().map(VisibilityConcealmentLease.Snapshot::id).toList());
        return expired;
    }

    public List<VisibilityConcealmentLease.Snapshot> rejectActive() {
        List<VisibilityConcealmentLease.Snapshot> rejected = leases.values().stream()
                .filter(VisibilityConcealmentLease::reject).map(VisibilityConcealmentLease::snapshot).toList();
        removeTerminal(rejected.stream().map(VisibilityConcealmentLease.Snapshot::id).toList());
        return rejected;
    }

    public void removeTerminal(Collection<VisibilityConcealmentLeaseId> ids) {
        for (VisibilityConcealmentLeaseId id : ids) {
            Optional.ofNullable(leases.get(id)).filter(VisibilityConcealmentLease::terminal)
                    .ifPresent(ignored -> leases.remove(id));
        }
    }
}
