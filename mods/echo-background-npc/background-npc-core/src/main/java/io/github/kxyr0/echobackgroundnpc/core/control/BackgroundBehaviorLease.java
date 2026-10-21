package io.github.kxyr0.echobackgroundnpc.core.control;

import io.github.kxyr0.echobackgroundnpc.api.task.TaskSource;
import java.util.Objects;

public record BackgroundBehaviorLease(String leaseId, TaskSource owner, long generation) {
    public BackgroundBehaviorLease {
        leaseId = Objects.requireNonNull(leaseId, "leaseId").trim();
        owner = Objects.requireNonNull(owner, "owner");
        if (leaseId.isEmpty() || generation < 0L) throw new IllegalArgumentException("Invalid behavior lease.");
    }
}
