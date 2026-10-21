package io.github.kxyr0.echobackgroundnpc.core.navigation;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan.RouteKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coalesces identical route requests and enforces a per-tick expensive-plan budget. */
public final class RouteRequestQueue {
    private final Map<RouteKey, List<BackgroundNpcId>> pending = new LinkedHashMap<>();

    public synchronized void request(RouteKey key, BackgroundNpcId agentId) {
        pending.computeIfAbsent(key, unused -> new ArrayList<>()).add(agentId);
    }

    public synchronized List<RouteDispatch> drain(int budget, BackgroundRoutePlanner planner,
            SharedRouteCache cache) {
        if (budget < 0) throw new IllegalArgumentException("budget cannot be negative.");
        List<RouteDispatch> dispatches = new ArrayList<>();
        var iterator = pending.entrySet().iterator();
        int computations = 0;
        while (iterator.hasNext()) {
            var request = iterator.next();
            Optional<SharedRoutePlan> cached = cache.get(request.getKey());
            if (cached.isEmpty() && computations >= budget) break;
            Optional<SharedRoutePlan> route = cached.isPresent() ? cached : planner.plan(request.getKey());
            if (cached.isEmpty()) computations++;
            route.ifPresent(cache::put);
            dispatches.add(new RouteDispatch(request.getKey(), request.getValue(), route, cached.isPresent()));
            iterator.remove();
        }
        return List.copyOf(dispatches);
    }

    public synchronized int queuedPlans() { return pending.size(); }
    public synchronized int queuedAgents() { return pending.values().stream().mapToInt(List::size).sum(); }

    public record RouteDispatch(RouteKey key, List<BackgroundNpcId> agents,
            Optional<SharedRoutePlan> route, boolean cacheHit) {
        public RouteDispatch { agents = List.copyOf(agents); }
    }
}
