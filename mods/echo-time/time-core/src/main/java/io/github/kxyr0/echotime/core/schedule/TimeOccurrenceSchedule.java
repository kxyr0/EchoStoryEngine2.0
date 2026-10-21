package io.github.kxyr0.echotime.core.schedule;

import io.github.kxyr0.echotime.api.identity.TimeOccurrenceId;
import io.github.kxyr0.echotime.api.schedule.TimeCrossingReason;
import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class TimeOccurrenceSchedule {
    private static final Comparator<TimeOccurrenceCrossed> ORDER = Comparator
            .comparingLong((TimeOccurrenceCrossed value) -> value.occurrence().absoluteMicroseconds())
            .thenComparing(Comparator.comparingInt(
                    (TimeOccurrenceCrossed value) -> value.entry().priority()).reversed())
            .thenComparing(value -> value.entry().selectorId());
    private final Map<String, TimeScheduleUpdate> providers = new LinkedHashMap<>();
    private NavigableMap<Long, List<TimeScheduleEntry>> daily = new TreeMap<>();
    private NavigableMap<Long, List<TimeScheduleEntry>> specific = new TreeMap<>();
    private int dailyEntryCount;

    public synchronized boolean replace(TimeScheduleUpdate update) {
        Objects.requireNonNull(update, "update");
        TimeScheduleUpdate previous = providers.get(update.providerId());
        if (previous != null && previous.catalogRevision() > update.catalogRevision()) return false;
        providers.put(update.providerId(), update);
        rebuild();
        return true;
    }

    public ScheduleCrossingResult crossed(
            EchoInstant previous,
            EchoInstant current,
            TimeCrossingReason reason,
            int maximum) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        if (maximum <= 0) throw new IllegalArgumentException("maximum must be positive.");
        if (current.absoluteMicroseconds() <= previous.absoluteMicroseconds()) {
            return new ScheduleCrossingResult(List.of(), false);
        }
        List<TimeOccurrenceCrossed> due = new ArrayList<>();
        if (!specific(previous, current, reason, maximum, due)) return ScheduleCrossingResult.exceeded();
        if (!daily(previous, current, reason, maximum, due)) return ScheduleCrossingResult.exceeded();
        due.sort(ORDER);
        return new ScheduleCrossingResult(due, false);
    }

    public synchronized List<TimeScheduleEntry> entries() {
        return providers.values().stream().flatMap(update -> update.entries().stream()).toList();
    }

    private boolean specific(EchoInstant old, EchoInstant now, TimeCrossingReason reason,
                             int maximum, List<TimeOccurrenceCrossed> due) {
        for (var bucket : specific.subMap(old.absoluteMicroseconds(), false,
                now.absoluteMicroseconds(), true).entrySet()) {
            if ((long) due.size() + bucket.getValue().size() > maximum) return false;
            for (TimeScheduleEntry entry : bucket.getValue()) {
                due.add(crossed(entry, bucket.getKey(), old, now, reason));
            }
        }
        return true;
    }

    private boolean daily(EchoInstant old, EchoInstant now, TimeCrossingReason reason,
                          int maximum, List<TimeOccurrenceCrossed> due) {
        if (daily.isEmpty()) return true;
        long oldDay = old.day().value(), nowDay = now.day().value();
        NavigableMap<Long, List<TimeScheduleEntry>> first = oldDay == nowDay
                ? daily.subMap(old.timeOfDay().microseconds(), false, now.timeOfDay().microseconds(), true)
                : daily.tailMap(old.timeOfDay().microseconds(), false);
        NavigableMap<Long, List<TimeScheduleEntry>> last = oldDay == nowDay
                ? java.util.Collections.emptyNavigableMap()
                : daily.headMap(now.timeOfDay().microseconds(), true);
        long fullDays = Math.max(0L, nowDay - oldDay - 1L);
        long count = rangeCount(first) + rangeCount(last);
        if (dailyEntryCount > 0 && fullDays > (maximum - due.size() - count) / dailyEntryCount) return false;
        count += fullDays * dailyEntryCount;
        if (count > maximum - due.size()) return false;
        appendDaily(first, oldDay, old, now, reason, due);
        for (long day = oldDay + 1L; day < nowDay; day++) appendDaily(daily, day, old, now, reason, due);
        appendDaily(last, nowDay, old, now, reason, due);
        return true;
    }

    private long rangeCount(NavigableMap<Long, List<TimeScheduleEntry>> range) {
        return range.values().stream().mapToLong(List::size).sum();
    }

    private void appendDaily(NavigableMap<Long, List<TimeScheduleEntry>> range, long day,
                             EchoInstant old, EchoInstant now, TimeCrossingReason reason,
                             List<TimeOccurrenceCrossed> due) {
        range.forEach((time, entries) -> entries.forEach(entry ->
                due.add(crossed(entry, absolute(day, time), old, now, reason))));
    }

    private TimeOccurrenceCrossed crossed(TimeScheduleEntry entry, long absolute,
                                           EchoInstant old, EchoInstant now, TimeCrossingReason reason) {
        EchoInstant target = new EchoInstant(absolute, now.revision());
        TimeOccurrenceId id = TimeOccurrenceId.derive(entry.selectorId(), target.day().value(),
                entry.time().microseconds(), entry.generation());
        return new TimeOccurrenceCrossed(id, entry, target, old, now, reason);
    }

    private long absolute(long day, long time) {
        return Math.addExact(Math.multiplyExact(day - 1L, TimeOfDay.MICROS_PER_DAY), time);
    }

    private synchronized void rebuild() {
        NavigableMap<Long, List<TimeScheduleEntry>> nextDaily = new TreeMap<>();
        NavigableMap<Long, List<TimeScheduleEntry>> nextSpecific = new TreeMap<>();
        providers.values().forEach(update -> update.entries().forEach(entry -> {
            if (entry.daily()) {
                add(nextDaily, entry.time().microseconds(), entry);
            } else {
                EchoDay day = entry.day().orElseThrow();
                add(nextSpecific, absolute(day.value(), entry.time().microseconds()), entry);
            }
        }));
        daily = immutable(nextDaily);
        specific = immutable(nextSpecific);
        dailyEntryCount = daily.values().stream().mapToInt(List::size).sum();
    }

    private void add(Map<Long, List<TimeScheduleEntry>> index, long key, TimeScheduleEntry entry) {
        index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
    }

    private NavigableMap<Long, List<TimeScheduleEntry>> immutable(
            NavigableMap<Long, List<TimeScheduleEntry>> source) {
        TreeMap<Long, List<TimeScheduleEntry>> copy = new TreeMap<>();
        source.forEach((key, entries) -> copy.put(key, List.copyOf(entries)));
        return java.util.Collections.unmodifiableNavigableMap(copy);
    }
}
