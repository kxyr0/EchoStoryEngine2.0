package io.github.kxyr0.echotrigger.core.reload;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record TriggerReloadPlan(List<Path> changedFiles, int parserCalls, int binderCalls) {
    public TriggerReloadPlan {
        changedFiles = List.copyOf(Objects.requireNonNull(changedFiles, "changedFiles"));
        if (parserCalls < 0 || binderCalls < 0) {
            throw new IllegalArgumentException("Call counters must not be negative.");
        }
    }
}
