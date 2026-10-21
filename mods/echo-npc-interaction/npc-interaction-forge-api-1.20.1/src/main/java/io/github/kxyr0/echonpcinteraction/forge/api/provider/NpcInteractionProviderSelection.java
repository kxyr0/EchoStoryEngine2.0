package io.github.kxyr0.echonpcinteraction.forge.api.provider;

import java.util.Objects;
import java.util.Optional;

public record NpcInteractionProviderSelection(
        Status status,
        int priority,
        int specificity,
        Optional<String> reason) {
    public NpcInteractionProviderSelection {
        status = Objects.requireNonNull(status, "status");
        reason = Objects.requireNonNull(reason, "reason");
        if (priority < 0 || specificity < 0) {
            throw new IllegalArgumentException("provider selection scores must not be negative.");
        }
    }

    public static NpcInteractionProviderSelection unsupported() {
        return new NpcInteractionProviderSelection(Status.UNSUPPORTED, 0, 0, Optional.empty());
    }

    public static NpcInteractionProviderSelection supported(int priority, int specificity) {
        return new NpcInteractionProviderSelection(Status.SUPPORTED, priority, specificity, Optional.empty());
    }

    public static NpcInteractionProviderSelection invalid(String reason) {
        return new NpcInteractionProviderSelection(Status.INVALID, 0, 0, Optional.of(reason));
    }

    public enum Status {
        UNSUPPORTED,
        SUPPORTED,
        INVALID
    }
}
