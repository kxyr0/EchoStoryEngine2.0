package io.github.kxyr0.echowardrobe.core.reload;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes incremental reload scope from previous and current fingerprints.
 */
public final class WardrobeReloadPlanner {
    public WardrobeReloadPlan plan(Map<String, String> previous, Map<String, String> current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Set<String> allSources = new LinkedHashSet<>();
        allSources.addAll(previous.keySet());
        allSources.addAll(current.keySet());
        var changed = new ArrayList<String>();
        var removed = new ArrayList<String>();
        var unchanged = new ArrayList<String>();
        for (String source : allSources) {
            if (!current.containsKey(source)) {
                removed.add(source);
            } else if (!Objects.equals(previous.get(source), current.get(source))) {
                changed.add(source);
            } else {
                unchanged.add(source);
            }
        }
        return new WardrobeReloadPlan(changed, removed, unchanged);
    }
}
