package io.github.kxyr0.echonavigation.forge.api.service;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.api.route.NavigationRoute;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/** Plans an Echo A* surface route without requiring a Mob or starting a movement controller. */
@FunctionalInterface
public interface NavigationSurfaceRouteService {
    PlanResult plan(PlanRequest request);

    record AgentProfile(double width, double height, double eyeHeight, double maximumStepUp,
            double maximumControlledDrop, double movementSpeed, boolean canUseLadders,
            boolean canOpenWoodenDoors, long revision) {
        public AgentProfile {
            if (width <= 0.0D || height <= 0.0D || eyeHeight <= 0.0D || maximumStepUp < 0.0D
                    || maximumControlledDrop < 0.0D || movementSpeed <= 0.0D || revision < 0L) {
                throw new IllegalArgumentException("Invalid surface route agent profile.");
            }
        }

        public static AgentProfile lightweightHumanoid() {
            return new AgentProfile(0.6D, 1.8D, 1.62D, 1.0D, 1.25D, 0.16D, true, true, 1L);
        }
    }

    record PlanRequest(NavigationMoveId moveId, NavigationSubjectId subjectId, ServerLevel level,
            Vector3 start, NavigationDestination destination, AgentProfile profile, long routeRevision) {
        public PlanRequest {
            moveId = Objects.requireNonNull(moveId, "moveId");
            subjectId = Objects.requireNonNull(subjectId, "subjectId");
            level = Objects.requireNonNull(level, "level");
            start = Objects.requireNonNull(start, "start");
            destination = Objects.requireNonNull(destination, "destination");
            profile = Objects.requireNonNull(profile, "profile");
            if (routeRevision < 1L) throw new IllegalArgumentException("routeRevision must be positive.");
        }
    }

    record PlanResult(Status status, Optional<NavigationRoute> route, int expandedNodes,
            long planningNanos, String diagnostic) {
        public PlanResult {
            status = Objects.requireNonNull(status, "status");
            route = Objects.requireNonNull(route, "route");
            diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
            if (expandedNodes < 0 || planningNanos < 0L) throw new IllegalArgumentException("Invalid metrics.");
        }
    }

    enum Status { ROUTE_FOUND, PATH_NOT_FOUND, SEARCH_LIMIT_REACHED, ROUTE_RECONSTRUCTION_FAILED }
}
