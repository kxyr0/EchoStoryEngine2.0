package io.github.kxyr0.echobackgroundnpc.core.schedule;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable global selector index used directly by Echo Time occurrence batches. */
public final class ScheduleCatalogIndex {
    private final Map<BackgroundNpcId, DailyScheduleIndex> schedules;
    private final Map<String, ScheduledAgentEntry> occurrences;

    public ScheduleCatalogIndex(BackgroundCatalog catalog) {
        Map<BackgroundNpcId, DailyScheduleIndex> byAgent = new LinkedHashMap<>();
        Map<String, ScheduledAgentEntry> bySelector = new LinkedHashMap<>();
        for (BackgroundNpcDefinition definition : catalog.definitions().values()) {
            DailyScheduleIndex schedule = new DailyScheduleIndex(definition);
            byAgent.put(definition.id(), schedule);
            for (BackgroundScheduleEntry entry : schedule.entries()) {
                ScheduledAgentEntry previous = bySelector.put(entry.selectorId(),
                        new ScheduledAgentEntry(definition.id(), entry));
                if (previous != null) throw new IllegalArgumentException("Duplicate selector: " + entry.selectorId());
            }
        }
        schedules = Map.copyOf(byAgent);
        occurrences = Map.copyOf(bySelector);
    }

    public DailyScheduleIndex schedule(BackgroundNpcId id) { return schedules.get(id); }
    public Optional<ScheduledAgentEntry> occurrence(String selectorId) {
        return Optional.ofNullable(occurrences.get(selectorId));
    }
    public Map<BackgroundNpcId, DailyScheduleIndex> schedules() { return schedules; }

    public record ScheduledAgentEntry(BackgroundNpcId agentId, BackgroundScheduleEntry entry) { }
}
