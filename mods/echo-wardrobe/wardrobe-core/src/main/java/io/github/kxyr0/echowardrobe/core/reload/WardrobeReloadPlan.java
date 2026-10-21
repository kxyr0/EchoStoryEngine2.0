package io.github.kxyr0.echowardrobe.core.reload;

import java.util.List;
import java.util.Objects;

/**
 * Incremental reload decision for changed, removed, and unchanged sources.
 */
public record WardrobeReloadPlan(List<String> changedSources, List<String> removedSources, List<String> unchangedSources) {
    public WardrobeReloadPlan {
        changedSources = List.copyOf(Objects.requireNonNull(changedSources, "changedSources"));
        removedSources = List.copyOf(Objects.requireNonNull(removedSources, "removedSources"));
        unchangedSources = List.copyOf(Objects.requireNonNull(unchangedSources, "unchangedSources"));
    }
}
