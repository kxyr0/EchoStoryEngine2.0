package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echobackgroundnpc.core.navigation.BackgroundRoutePlanner;
import io.github.kxyr0.echobackgroundnpc.core.navigation.RouteRequestQueue;
import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRouteCache;
import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import io.github.kxyr0.echobackgroundnpc.core.navigation.LocalSeparationGrid;
import io.github.kxyr0.echobackgroundnpc.core.simulation.BackgroundAgentRuntime;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSnapshot;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSyncTracker;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import io.github.kxyr0.echobackgroundnpc.forge.network.BackgroundNpcNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Budgeted shared-route movement motor for render-only entities. */
public final class BackgroundMovementDriver {
    private final BackgroundAgentRuntime runtime;
    private final MaterializedBodies bodies;
    private final BackgroundRoutePlanner planner;
    private final Consumer<BackgroundAgentState> transitioned;
    private final SharedRouteCache cache = new SharedRouteCache(256);
    private final RouteRequestQueue queue = new RouteRequestQueue();
    private final Map<BackgroundNpcId, Motion> motions = new HashMap<>();
    private final LocalSeparationGrid separation = new LocalSeparationGrid(2.0D);
    private final MovementSyncTracker sync = new MovementSyncTracker(100L, 0.01D);
    private long routeComputations;
    private long neighborChecks;

    public BackgroundMovementDriver(BackgroundAgentRuntime runtime, MaterializedBodies bodies,
            BackgroundRoutePlanner planner, Consumer<BackgroundAgentState> transitioned) {
        this.runtime = runtime;
        this.bodies = bodies;
        this.planner = planner;
        this.transitioned = transitioned;
    }

    public void tick(long serverTick) {
        rebuildSeparation();
        runtime.states().stream().filter(value -> value.task() instanceof BackgroundTask.Travel)
                .filter(value -> runtime.leases().current(value.id()).isEmpty())
                .filter(value -> bodies.find(value.id()).isPresent()).forEach(this::ensureRoute);
        queue.drain(2, planner, cache).forEach(dispatch -> {
            if (!dispatch.cacheHit()) routeComputations++;
            dispatch.route().ifPresent(route -> dispatch.agents().forEach(id -> motions.put(id,
                    new Motion(route, 1))));
        });
        runtime.states().forEach(state -> move(state, serverTick));
    }

    public int queuedRoutes() { return queue.queuedPlans(); }
    public double cacheHitRate() { return cache.hitRate(); }
    public long routeComputations() { return routeComputations; }
    public long neighborChecks() { return neighborChecks; }
    public void invalidate(long revision) { cache.invalidateBefore(revision); motions.clear(); sync.clear(); }

    private void ensureRoute(BackgroundAgentState state) {
        BackgroundTask.Travel travel = (BackgroundTask.Travel) state.task();
        Motion active = motions.get(state.id());
        if (active != null && active.route().key().targetAnchor().equals(travel.destination().value())) return;
        SharedRoutePlan.RouteKey key = new SharedRoutePlan.RouteKey(runtime.catalog().revision(),
                state.logicalLocation().value(), travel.destination().value(), "human");
        var cached = cache.get(key);
        if (cached.isPresent()) {
            motions.put(state.id(), new Motion(cached.orElseThrow(), 1));
        } else {
            queue.request(key, state.id());
        }
    }

    private void move(BackgroundAgentState state, long serverTick) {
        int interval = state.lod().updateIntervalTicks();
        if (interval == 0 || serverTick % interval != 0) return;
        if (runtime.leases().current(state.id()).isPresent()) return;
        BackgroundNpcEntity entity = bodies.find(state.id()).orElse(null);
        Motion motion = motions.get(state.id());
        if (entity == null || motion == null) return;
        WorldPoint target = motion.route().points().get(motion.pointIndex());
        double dx = target.x() - entity.getX();
        double dy = target.y() - entity.getY();
        double dz = target.z() - entity.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.18) { advance(state, motion, serverTick); return; }
        double step = Math.min(distance, speed(state) * interval / 20.0);
        Vec3 delta = separate(state.id(), entity, new Vec3(dx / distance * step, dy / distance * step,
                dz / distance * step));
        entity.move(MoverType.SELF, delta);
        entity.setYRot((float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F);
        entity.hasImpulse = true;
        if (serverTick % (interval * 2L) == 0L) sendSnapshot(state, entity, delta, serverTick);
    }

    private void advance(BackgroundAgentState state, Motion motion, long serverTick) {
        if (motion.pointIndex() + 1 < motion.route().points().size()) {
            motions.put(state.id(), new Motion(motion.route(), motion.pointIndex() + 1));
            return;
        }
        motions.remove(state.id());
        runtime.arrive(state.id(), ((BackgroundTask.Travel) state.task()).destination());
        runtime.state(state.id()).ifPresent(current -> {
            transitioned.accept(current);
            bodies.find(current.id()).ifPresent(entity -> sendSnapshot(current, entity, Vec3.ZERO, serverTick));
        });
    }

    private double speed(BackgroundAgentState state) {
        BackgroundNpcDefinition definition = runtime.catalog().definitions().get(state.id());
        if (definition.walkSpeed().isPresent()) return definition.walkSpeed().orElseThrow();
        return definition.profile().map(runtime.catalog().profiles()::get).map(value -> value.walkSpeed()).orElse(1.0);
    }

    private void rebuildSeparation() {
        var points = runtime.states().stream().map(state -> bodies.find(state.id()).map(entity ->
                new LocalSeparationGrid.AgentPoint(state.id(), new WorldPoint(
                        entity.level().dimension().location().toString(), entity.getX(), entity.getY(),
                        entity.getZ())))).flatMap(java.util.Optional::stream).toList();
        separation.rebuild(points);
    }

    private Vec3 separate(BackgroundNpcId id, BackgroundNpcEntity entity, Vec3 intended) {
        WorldPoint here = new WorldPoint(entity.level().dimension().location().toString(), entity.getX(),
                entity.getY(), entity.getZ());
        double pushX = 0.0D; double pushZ = 0.0D;
        for (var neighbor : separation.neighbors(here)) {
            if (neighbor.id().equals(id)) continue;
            neighborChecks++;
            double dx = here.x() - neighbor.point().x();
            double dz = here.z() - neighbor.point().z();
            double squared = dx * dx + dz * dz;
            if (squared > 0.0001D && squared < 1.44D) {
                double scale = (1.2D - Math.sqrt(squared)) / Math.sqrt(squared) * 0.08D;
                pushX += dx * scale; pushZ += dz * scale;
            }
        }
        Vec3 result = intended.add(pushX, 0.0D, pushZ);
        double horizontal = Math.sqrt(result.x * result.x + result.z * result.z);
        double intendedHorizontal = Math.sqrt(intended.x * intended.x + intended.z * intended.z);
        return horizontal > intendedHorizontal && horizontal > 0.0D
                ? new Vec3(result.x / horizontal * intendedHorizontal, result.y,
                        result.z / horizontal * intendedHorizontal) : result;
    }

    private void sendSnapshot(BackgroundAgentState state, BackgroundNpcEntity entity, Vec3 velocity,
            long serverTick) {
        var position = new WorldPoint(entity.level().dimension().location().toString(), entity.getX(),
                entity.getY(), entity.getZ());
        var snapshot = new MovementSnapshot(state.id(), position, velocity.x, velocity.y, velocity.z,
                entity.getYRot(), state.task().kind(), serverTick, serverTick);
        if (sync.shouldSend(snapshot)) BackgroundNpcNetwork.movement(entity, snapshot);
    }

    private record Motion(SharedRoutePlan route, int pointIndex) { }
}
