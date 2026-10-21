package io.github.kxyr0.echobackgroundnpc.core.control;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Priority-aware control ownership; equal priority needs the same lease identity. */
public final class BehaviorLeaseRegistry {
    private final Map<BackgroundNpcId, BackgroundBehaviorLease> leases = new HashMap<>();

    public synchronized boolean acquire(BackgroundNpcId id, BackgroundBehaviorLease candidate) {
        BackgroundBehaviorLease current = leases.get(id);
        if (current != null && current.owner().priority() > candidate.owner().priority()) return false;
        if (current != null && current.owner() == candidate.owner()
                && !current.leaseId().equals(candidate.leaseId())) return false;
        leases.put(id, candidate);
        return true;
    }

    public synchronized boolean release(BackgroundNpcId id, String leaseId) {
        BackgroundBehaviorLease current = leases.get(id);
        if (current == null || !current.leaseId().equals(leaseId)) return false;
        leases.remove(id);
        return true;
    }

    public synchronized Optional<BackgroundBehaviorLease> current(BackgroundNpcId id) {
        return Optional.ofNullable(leases.get(id));
    }
}
