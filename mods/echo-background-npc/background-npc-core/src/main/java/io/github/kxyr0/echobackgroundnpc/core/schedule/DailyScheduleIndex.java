package io.github.kxyr0.echobackgroundnpc.core.schedule;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sorted daily occurrences and O(1) selector lookup; no per-agent time polling. */
public final class DailyScheduleIndex {
    private final List<BackgroundScheduleEntry> entries;
    private final Map<String, BackgroundScheduleEntry> bySelector;

    public DailyScheduleIndex(BackgroundNpcDefinition definition) {
        entries = definition.schedule().stream().sorted().toList();
        bySelector = entries.stream().collect(Collectors.toUnmodifiableMap(
                BackgroundScheduleEntry::selectorId, Function.identity()));
        for (int index = 1; index < entries.size(); index++) {
            if (entries.get(index - 1).minuteOfDay() == entries.get(index).minuteOfDay()) {
                throw new IllegalArgumentException("Conflicting schedule entries at minute "
                        + entries.get(index).minuteOfDay());
            }
        }
    }

    public List<BackgroundScheduleEntry> entries() { return entries; }
    public Optional<BackgroundScheduleEntry> selector(String selectorId) {
        return Optional.ofNullable(bySelector.get(selectorId));
    }

    /** Late load and Time.Set resolve the latest stateful phase and never replay one-shots. */
    public ResolvedSchedulePhase resolveState(int minuteOfDay, BackgroundTask fallback) {
        BackgroundScheduleEntry selected = null;
        int selectedIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            BackgroundScheduleEntry candidate = entries.get(index);
            if (candidate.minuteOfDay() > minuteOfDay) break;
            if (!candidate.oneShot()) {
                selected = candidate;
                selectedIndex = index;
            }
        }
        return selected == null ? new ResolvedSchedulePhase(-1, fallback, Optional.empty())
                : new ResolvedSchedulePhase(selectedIndex, selected.task(), Optional.of(selected));
    }

    public Optional<BackgroundScheduleEntry> next(int minuteOfDay) {
        return entries.stream().filter(entry -> entry.minuteOfDay() > minuteOfDay).findFirst()
                .or(() -> entries.stream().findFirst());
    }
}
