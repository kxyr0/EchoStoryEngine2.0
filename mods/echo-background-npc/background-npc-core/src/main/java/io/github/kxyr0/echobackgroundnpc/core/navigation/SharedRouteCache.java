package io.github.kxyr0.echobackgroundnpc.core.navigation;

import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan.RouteKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Revision-aware bounded LRU of immutable route plans. */
public final class SharedRouteCache {
    private final int capacity;
    private final Map<RouteKey, SharedRoutePlan> routes;
    private long hits;
    private long misses;

    public SharedRouteCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive.");
        this.capacity = capacity;
        routes = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    public synchronized Optional<SharedRoutePlan> get(RouteKey key) {
        SharedRoutePlan plan = routes.get(key);
        if (plan == null) misses++; else hits++;
        return Optional.ofNullable(plan);
    }

    public synchronized void put(SharedRoutePlan plan) {
        routes.put(plan.key(), plan);
        while (routes.size() > capacity) routes.remove(routes.keySet().iterator().next());
    }

    public synchronized void invalidateBefore(long revision) {
        routes.keySet().removeIf(key -> key.navigationRevision() < revision);
    }

    public synchronized double hitRate() {
        long total = hits + misses;
        return total == 0L ? 0.0 : (double) hits / total;
    }
    public synchronized int size() { return routes.size(); }
}
