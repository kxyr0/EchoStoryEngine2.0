package io.github.kxyr0.echobackgroundnpc.navigation;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.core.navigation.BackgroundRoutePlanner;
import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolution;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationRouteBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationSurfaceRouteService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/** Echo Navigation boundary for shared named-anchor route requests. */
public final class EchoNavigationBackgroundPlanner implements BackgroundRoutePlanner {
    private final MinecraftServer server;
    private final ExactLocationRegistry locations;

    public EchoNavigationBackgroundPlanner(MinecraftServer server, ExactLocationRegistry locations) {
        this.server = server;
        this.locations = locations;
    }

    @Override
    public Optional<SharedRoutePlan> plan(SharedRoutePlan.RouteKey key) {
        var routeService = EchoNavigationRouteBridge.lookup(server);
        if (routeService.isEmpty()) return Optional.empty();
        BackgroundLocationResolution start = locations.resolve(server, new BackgroundLocationId(key.startAnchor()));
        BackgroundLocationResolution target = locations.resolve(server, new BackgroundLocationId(key.targetAnchor()));
        if (start.status() != BackgroundLocationResolution.Status.FOUND
                || target.status() != BackgroundLocationResolution.Status.FOUND
                || !start.dimension().equals(target.dimension())) return Optional.empty();
        String dimension = start.dimension().orElseThrow().location().toString();
        var first = start.position().orElseThrow();
        var last = target.position().orElseThrow();
        var level = server.getLevel(start.dimension().orElseThrow());
        if (level == null) return Optional.empty();
        String token = Integer.toUnsignedString(key.hashCode());
        var request = new NavigationSurfaceRouteService.PlanRequest(
                new NavigationMoveId("echo_background_npc:shared_" + token),
                new NavigationSubjectId("echo_background_npc:profile_" + token), level,
                new Vector3(first.getX() + 0.5D, first.getY(), first.getZ() + 0.5D),
                new NavigationDestination(dimension, last.getX(), last.getY(), last.getZ()),
                NavigationSurfaceRouteService.AgentProfile.lightweightHumanoid(), key.navigationRevision() + 1L);
        var planned = routeService.orElseThrow().plan(request);
        if (planned.status() != NavigationSurfaceRouteService.Status.ROUTE_FOUND) return Optional.empty();
        List<WorldPoint> points = new ArrayList<>();
        planned.route().orElseThrow().nodes().forEach(node -> points.add(new WorldPoint(dimension,
                node.stancePosition().x(), node.stancePosition().y(), node.stancePosition().z())));
        if (points.size() == 1) points.add(new WorldPoint(dimension, last.getX() + 0.5D, last.getY(), last.getZ() + 0.5D));
        return Optional.of(new SharedRoutePlan(key, points, length(points)));
    }

    private double length(List<WorldPoint> points) {
        double total = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            total += Math.sqrt(points.get(index - 1).distanceSquared(points.get(index)));
        }
        return total;
    }
}
