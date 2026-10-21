package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.forge.api.service.BackgroundNpcService;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class ForgeBackgroundService implements BackgroundNpcService {
    private final MinecraftServer server;
    private final ForgeBackgroundPopulation population;

    public ForgeBackgroundService(MinecraftServer server, ForgeBackgroundPopulation population) {
        this.server = server;
        this.population = population;
    }
    @Override public List<BackgroundAgentState> agents() { return population.agents().states(); }
    @Override public Optional<BackgroundAgentState> find(BackgroundNpcId id) { return population.agents().state(id); }
    @Override public boolean reload() {
        server.reloadResources(server.getPackRepository().getSelectedIds());
        return true;
    }
    @Override public String performanceReport() {
        long virtual = agents().stream().filter(value -> value.materialization() ==
                io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState.VIRTUAL).count();
        return "total=" + agents().size() + " virtual=" + virtual + " materialized="
                + population.materializedCount() + " queued_routes=" + population.queuedRoutes()
                + " route_computations=" + population.routeComputations()
                + " avoidance_neighbor_checks=" + population.neighborChecks()
                + " route_cache_hit=" + String.format(java.util.Locale.ROOT, "%.1f%%",
                        population.routeHitRate() * 100.0);
    }
    public List<BackgroundAgentState> nearby(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.phys.Vec3 point, double range) {
        return population.nearby(level, point, range);
    }
    public java.util.List<io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry> schedule(
            BackgroundNpcId id) {
        var value = population.agents().schedules().schedule(id);
        return value == null ? List.of() : value.entries();
    }
}
