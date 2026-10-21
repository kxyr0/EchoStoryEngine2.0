package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.materialization.MaterializationPolicy;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import io.github.kxyr0.echobackgroundnpc.core.simulation.BackgroundAgentRuntime;
import io.github.kxyr0.echobackgroundnpc.core.simulation.ScheduledConversation;
import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolution;
import io.github.kxyr0.echobackgroundnpc.forge.api.quest.AmbientQuestDispatcher;
import io.github.kxyr0.echobackgroundnpc.navigation.EchoNavigationBackgroundPlanner;
import io.github.kxyr0.echobackgroundnpc.navigation.ExactLocationRegistry;
import io.github.kxyr0.echobackgroundnpc.time.EchoTimeBackgroundAdapter;
import io.github.kxyr0.echobackgroundnpc.animation.EchoBackgroundAnimationAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/** Forge lifecycle shell around the pure logical runtime. */
public final class ForgeBackgroundPopulation {
    private final MinecraftServer server;
    private final Logger logger;
    private final MaterializationPolicy policy = MaterializationPolicy.defaults();
    private final ExactLocationRegistry locationRegistry = new ExactLocationRegistry();
    private final Map<BackgroundLocationId, BackgroundLocationResolution> locations = new HashMap<>();
    private final MaterializedBodies bodies;
    private final BackgroundAgentRuntime agents;
    private final BackgroundMovementDriver movement;
    private final EchoTimeBackgroundAdapter time;
    private final EchoBackgroundAnimationAdapter animation = new EchoBackgroundAnimationAdapter();
    private final AmbientConversationController conversations;

    public ForgeBackgroundPopulation(MinecraftServer server, BackgroundCatalog catalog, int minuteOfDay,
            AmbientQuestDispatcher quests, Logger logger) {
        this.server = server;
        this.logger = logger;
        agents = new BackgroundAgentRuntime(catalog, minuteOfDay);
        bodies = new MaterializedBodies(server);
        movement = new BackgroundMovementDriver(agents, bodies,
                new EchoNavigationBackgroundPlanner(server, locationRegistry), this::animate);
        conversations = new AmbientConversationController(server, agents, bodies,
                new AmbientConversationController.ConversationAnimator() {
                    @Override public void pose(BackgroundNpcId id, boolean speaking) {
                        animateConversation(id, speaking);
                    }
                    @Override public void restore(BackgroundNpcId id) { agents.state(id).ifPresent(state -> {
                        bodies.find(id).ifPresent(entity -> entity.apply(state)); animate(state);
                    }); }
                }, quests, logger);
        time = new EchoTimeBackgroundAdapter(agents, this::crossed);
        time.publish(server);
    }

    public void replaceCatalog(BackgroundCatalog catalog, int minuteOfDay) {
        conversations.close();
        agents.replaceCatalog(catalog, minuteOfDay).forEach(this::transitioned);
        locations.clear();
        movement.invalidate(catalog.revision());
        time.publish(server);
    }

    public void tick(long serverTick) {
        if (serverTick % 4L == 0L) reconcilePresence();
        conversations.tick(serverTick);
        if (bodies.count() > 0) movement.tick(serverTick);
    }

    public void occurrence(io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent event) {
        time.occurrence(event);
    }
    public void lifecycle(io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecycleEvent event) {
        conversations.close();
        time.lifecycle(event);
    }
    public BackgroundAgentRuntime agents() { return agents; }
    public MinecraftServer server() { return server; }
    public int materializedCount() { return bodies.count(); }
    public int queuedRoutes() { return movement.queuedRoutes(); }
    public double routeHitRate() { return movement.cacheHitRate(); }
    public long routeComputations() { return movement.routeComputations(); }
    public long neighborChecks() { return movement.neighborChecks(); }
    public java.util.List<BackgroundAgentState> nearby(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.phys.Vec3 point, double range) {
        double rangeSquared = range * range;
        return agents.states().stream().filter(state -> bodies.find(state.id())
                .filter(entity -> entity.level() == level && entity.distanceToSqr(point) <= rangeSquared).isPresent())
                .toList();
    }
    public void close() { conversations.close(); bodies.clear(); }

    private void reconcilePresence() {
        for (BackgroundAgentState state : agents.states()) {
            Optional<WorldPoint> point = point(state.logicalLocation());
            if (point.isEmpty()) {
                if (state.materialization() == MaterializationState.MATERIALIZED) dematerialize(state.id());
                continue;
            }
            double distance = bodies.nearestPlayerDistance(point.orElseThrow());
            var presence = policy.classify(state.materialization(), distance);
            if (presence.materialization() == MaterializationState.MATERIALIZED) {
                boolean newBody = bodies.find(state.id()).isEmpty();
                if (bodies.materialize(state, point.orElseThrow())
                        && (state.materialization() != MaterializationState.MATERIALIZED
                        || state.lod() != presence.lod())) {
                    agents.updatePresence(state.id(), MaterializationState.MATERIALIZED, presence.lod());
                    bodies.find(state.id()).ifPresent(entity -> entity.apply(agents.state(state.id()).orElseThrow()));
                    if (newBody) animate(agents.state(state.id()).orElseThrow());
                }
            } else {
                if (state.materialization() == MaterializationState.MATERIALIZED
                        || bodies.find(state.id()).isPresent()) dematerialize(state.id());
            }
        }
    }

    private void dematerialize(BackgroundNpcId id) {
        bodies.dematerialize(id);
        agents.updatePresence(id, MaterializationState.VIRTUAL,
                io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundSimulationLod.VIRTUAL);
    }

    private Optional<WorldPoint> point(BackgroundLocationId id) {
        BackgroundLocationResolution resolution = locations.computeIfAbsent(id,
                value -> locationRegistry.resolve(server, value));
        if (resolution.status() != BackgroundLocationResolution.Status.FOUND) return Optional.empty();
        var block = resolution.position().orElseThrow();
        return Optional.of(new WorldPoint(resolution.dimension().orElseThrow().location().toString(),
                block.getX() + 0.5, block.getY(), block.getZ() + 0.5));
    }

    private void crossed(BackgroundAgentRuntime.CrossingResult result) {
        result.transition().ifPresent(value -> {
            conversations.preemptFor(value.current().id());
            transitioned(value);
        });
        result.conversation().ifPresent(this::conversation);
    }

    private void conversation(ScheduledConversation value) {
        conversations.start(value, server.overworld().getGameTime());
    }

    private void logTransition(io.github.kxyr0.echobackgroundnpc.core.simulation.AgentTransition value) {
        logger.info("[BG_NPC] Agent={} previous={} next={} source={} owner={}", value.current().id(),
                value.previous().task().kind(), value.current().task().kind(),
                value.source().map(Object::toString).orElse("<runtime>"), value.current().owner());
    }

    private void transitioned(io.github.kxyr0.echobackgroundnpc.core.simulation.AgentTransition value) {
        logTransition(value);
        bodies.find(value.current().id()).ifPresent(entity -> entity.apply(value.current()));
        animate(value.current());
    }

    private void animate(BackgroundAgentState state) {
        var definition = agents.catalog().definitions().get(state.id());
        if (definition == null) return;
        animation.transition(server, state, animationProfile(state.id()));
    }

    private void animateConversation(BackgroundNpcId id, boolean speaking) {
        agents.state(id).ifPresent(state -> animation.conversation(server, state, animationProfile(id), speaking));
    }

    private String animationProfile(BackgroundNpcId id) {
        var definition = agents.catalog().definitions().get(id);
        return definition == null ? "jdh:standard_humanoid" : definition.profile()
                .map(agents.catalog().profiles()::get).map(value -> value.locomotionProfile())
                .orElse("jdh:standard_humanoid");
    }
}
