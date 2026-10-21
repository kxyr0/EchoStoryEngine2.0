package io.github.kxyr0.echotime.core.pause;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseLease;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TimePauseRegistry {
    private final Map<TimePauseOwner, TimePauseLease> leases = new LinkedHashMap<>();

    public PauseLeaseChange acquire(TimePauseOwner owner, TimeMutationId mutationId) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(mutationId, "mutationId");
        TimePauseLease existing = leases.get(owner);
        if (existing != null) {
            return new PauseLeaseChange(PauseLeaseChange.Status.ALREADY_OWNED, Optional.of(existing));
        }
        TimePauseLease lease = new TimePauseLease(owner, mutationId);
        leases.put(owner, lease);
        return new PauseLeaseChange(PauseLeaseChange.Status.ACQUIRED, Optional.of(lease));
    }

    public PauseLeaseChange release(TimePauseOwner owner) {
        TimePauseLease removed = leases.remove(Objects.requireNonNull(owner, "owner"));
        return removed == null
                ? new PauseLeaseChange(PauseLeaseChange.Status.NOT_OWNED, Optional.empty())
                : new PauseLeaseChange(PauseLeaseChange.Status.RELEASED, Optional.of(removed));
    }

    public boolean paused() {
        return !leases.isEmpty();
    }

    public List<TimePauseLease> leases() {
        return leases.values().stream().sorted(java.util.Comparator.comparing(TimePauseLease::owner)).toList();
    }

    public void restore(List<TimePauseLease> restored) {
        leases.clear();
        Objects.requireNonNull(restored, "restored").forEach(lease -> leases.put(lease.owner(), lease));
    }
}
