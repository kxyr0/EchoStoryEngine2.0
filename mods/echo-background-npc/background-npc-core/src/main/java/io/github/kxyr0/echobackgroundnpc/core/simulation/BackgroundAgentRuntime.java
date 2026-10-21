package io.github.kxyr0.echobackgroundnpc.core.simulation;

import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.AgentLifeState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundSimulationLod;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskSource;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.catalog.DeterministicAppearanceResolver;
import io.github.kxyr0.echobackgroundnpc.core.control.BehaviorLeaseRegistry;
import io.github.kxyr0.echobackgroundnpc.core.schedule.OccurrenceLedger;
import io.github.kxyr0.echobackgroundnpc.core.schedule.ResolvedSchedulePhase;
import io.github.kxyr0.echobackgroundnpc.core.schedule.ScheduleCatalogIndex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-scoped lightweight logical population; it owns no Minecraft Mob AI. */
public final class BackgroundAgentRuntime {
    private final DeterministicAppearanceResolver appearances = new DeterministicAppearanceResolver();
    private final BehaviorLeaseRegistry leases = new BehaviorLeaseRegistry();
    private final OccurrenceLedger occurrences = new OccurrenceLedger(8192);
    private final Map<BackgroundNpcId, BackgroundAgentState> agents = new LinkedHashMap<>();
    private BackgroundCatalog catalog;
    private ScheduleCatalogIndex schedules;

    public BackgroundAgentRuntime(BackgroundCatalog catalog, int minuteOfDay) {
        replaceCatalog(catalog, minuteOfDay);
    }

    public synchronized List<AgentTransition> replaceCatalog(BackgroundCatalog next, int minuteOfDay) {
        catalog = next;
        schedules = new ScheduleCatalogIndex(next);
        List<AgentTransition> transitions = new ArrayList<>();
        agents.keySet().retainAll(next.definitions().keySet());
        next.definitions().values().forEach(definition -> {
            BackgroundAgentState previous = agents.get(definition.id());
            BackgroundAgentState current = reconstruct(definition, previous, minuteOfDay);
            agents.put(definition.id(), current);
            if (previous != null && !previous.equals(current)) {
                transitions.add(new AgentTransition(previous, current, "DEFINITION_COMMIT",
                        Optional.of(definition.source())));
            }
        });
        return List.copyOf(transitions);
    }

    public synchronized List<AgentTransition> recomputeAll(int minuteOfDay, String reason) {
        List<AgentTransition> transitions = new ArrayList<>();
        catalog.definitions().values().forEach(definition -> {
            BackgroundAgentState previous = agents.get(definition.id());
            if (previous.life() == AgentLifeState.DEAD || leases.current(definition.id()).isPresent()) return;
            BackgroundAgentState current = reconstruct(definition, previous, minuteOfDay);
            agents.put(definition.id(), current);
            if (!previous.equals(current)) transitions.add(new AgentTransition(previous, current, reason,
                    Optional.of(definition.source())));
        });
        return List.copyOf(transitions);
    }

    public synchronized CrossingResult cross(String selectorId, String occurrenceId, boolean presentable) {
        Optional<ScheduleCatalogIndex.ScheduledAgentEntry> selected = schedules.occurrence(selectorId);
        if (selected.isEmpty()) return CrossingResult.empty();
        var scheduled = selected.orElseThrow();
        BackgroundAgentState previous = agents.get(scheduled.agentId());
        if (previous == null || previous.life() == AgentLifeState.DEAD) return CrossingResult.empty();
        if (scheduled.entry().task() instanceof BackgroundTask.Converse converse) {
            if (!occurrences.first(occurrenceId)) return CrossingResult.empty();
            ConversationOutcome outcome = presentable ? ConversationOutcome.COMPLETED
                    : ConversationOutcome.SKIPPED_VIRTUAL;
            return new CrossingResult(Optional.empty(), Optional.of(new ScheduledConversation(scheduled.agentId(),
                    converse, scheduled.entry(), occurrenceId, outcome)));
        }
        if (leases.current(scheduled.agentId()).filter(value -> value.owner() == TaskSource.STORY_OVERRIDE).isPresent()) {
            return CrossingResult.empty();
        }
        BackgroundAgentState current = apply(previous, scheduled.entry().task(), TaskSource.SCHEDULE,
                phaseOf(scheduled.agentId(), scheduled.entry().selectorId()));
        agents.put(scheduled.agentId(), current);
        AgentTransition transition = new AgentTransition(previous, current, "TIME_CROSSED",
                Optional.of(scheduled.entry().source()));
        return new CrossingResult(Optional.of(transition), Optional.empty());
    }

    public synchronized Optional<BackgroundAgentState> state(BackgroundNpcId id) {
        return Optional.ofNullable(agents.get(id));
    }
    public synchronized List<BackgroundAgentState> states() { return List.copyOf(agents.values()); }
    public BackgroundCatalog catalog() { return catalog; }
    public ScheduleCatalogIndex schedules() { return schedules; }
    public BehaviorLeaseRegistry leases() { return leases; }
    public OccurrenceLedger occurrences() { return occurrences; }

    public synchronized void updatePresence(BackgroundNpcId id, MaterializationState materialization,
            BackgroundSimulationLod lod) {
        BackgroundAgentState state = agents.get(id);
        if (state == null) return;
        agents.put(id, new BackgroundAgentState(id, state.life(), materialization, lod, state.logicalLocation(),
                state.task(), state.owner(), state.appearance(), state.schedulePhase(), state.routeProgress(),
                state.revision() + 1L));
    }

    public synchronized void markDead(BackgroundNpcId id) {
        BackgroundAgentState state = agents.get(id);
        if (state == null) return;
        agents.put(id, new BackgroundAgentState(id, AgentLifeState.DEAD, MaterializationState.VIRTUAL,
                BackgroundSimulationLod.VIRTUAL, state.logicalLocation(), state.task(), state.owner(),
                state.appearance(), state.schedulePhase(), state.routeProgress(), state.revision() + 1L));
    }

    public synchronized void arrive(BackgroundNpcId id,
            io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId location) {
        BackgroundAgentState state = agents.get(id);
        if (state == null) return;
        agents.put(id, new BackgroundAgentState(id, state.life(), state.materialization(), state.lod(), location,
                state.task(), state.owner(), state.appearance(), state.schedulePhase(), 1.0,
                state.revision() + 1L));
    }

    private BackgroundAgentState reconstruct(BackgroundNpcDefinition definition, BackgroundAgentState previous,
            int minuteOfDay) {
        BackgroundTask fallback = new BackgroundTask.Stay(definition.home());
        ResolvedSchedulePhase phase = schedules.schedule(definition.id()).resolveState(minuteOfDay, fallback);
        MaterializationState materialization = previous == null ? MaterializationState.VIRTUAL
                : previous.materialization();
        BackgroundSimulationLod lod = previous == null ? BackgroundSimulationLod.VIRTUAL : previous.lod();
        AgentLifeState life = previous == null ? AgentLifeState.ALIVE : previous.life();
        var location = phase.task() instanceof BackgroundTask.Travel travel ? travel.destination()
                : phase.entry().flatMap(entry -> target(entry.task())).orElse(definition.home());
        long revision = previous == null ? 0L : previous.revision() + 1L;
        return new BackgroundAgentState(definition.id(), life, materialization, lod, location, phase.task(),
                TaskSource.SCHEDULE, appearances.resolve(definition, catalog), phase.index(), 1.0, revision);
    }

    private BackgroundAgentState apply(BackgroundAgentState previous, BackgroundTask task, TaskSource owner,
            int phase) {
        var location = previous.logicalLocation();
        double progress = 0.0;
        if (task instanceof BackgroundTask.Stay stay) { location = stay.location(); progress = 1.0; }
        if (task instanceof BackgroundTask.Perform perform && perform.location().isPresent()) {
            location = perform.location().orElseThrow();
        }
        if (previous.materialization() == MaterializationState.VIRTUAL && task instanceof BackgroundTask.Travel travel) {
            location = travel.destination();
            progress = 1.0;
        }
        return new BackgroundAgentState(previous.id(), previous.life(), previous.materialization(), previous.lod(),
                location, task, owner, previous.appearance(), phase, progress, previous.revision() + 1L);
    }

    private int phaseOf(BackgroundNpcId id, String selectorId) {
        var entries = schedules.schedule(id).entries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).selectorId().equals(selectorId)) return index;
        }
        return -1;
    }

    private Optional<io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId> target(
            BackgroundTask task) {
        if (task instanceof BackgroundTask.Stay stay) return Optional.of(stay.location());
        if (task instanceof BackgroundTask.Travel travel) return Optional.of(travel.destination());
        if (task instanceof BackgroundTask.Perform perform) return perform.location();
        return ((BackgroundTask.Converse) task).rendezvous();
    }

    public record CrossingResult(Optional<AgentTransition> transition,
            Optional<ScheduledConversation> conversation) {
        public static CrossingResult empty() { return new CrossingResult(Optional.empty(), Optional.empty()); }
    }
}
