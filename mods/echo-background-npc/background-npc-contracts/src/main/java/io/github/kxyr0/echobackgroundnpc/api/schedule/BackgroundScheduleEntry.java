package io.github.kxyr0.echobackgroundnpc.api.schedule;

import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.Objects;

/** One daily schedule phase/event at a minute of the narrative day. */
public record BackgroundScheduleEntry(
        int minuteOfDay,
        BackgroundTask task,
        String selectorId,
        BackgroundSourceRef source
) implements Comparable<BackgroundScheduleEntry> {
    public BackgroundScheduleEntry {
        if (minuteOfDay < 0 || minuteOfDay >= 24 * 60) throw new IllegalArgumentException("Invalid minute.");
        task = Objects.requireNonNull(task, "task");
        selectorId = Objects.requireNonNull(selectorId, "selectorId").trim();
        source = Objects.requireNonNull(source, "source");
        if (selectorId.isEmpty()) throw new IllegalArgumentException("selectorId cannot be blank.");
    }

    public boolean oneShot() { return task instanceof BackgroundTask.Converse; }
    @Override public int compareTo(BackgroundScheduleEntry other) {
        int time = Integer.compare(minuteOfDay, other.minuteOfDay);
        return time == 0 ? selectorId.compareTo(other.selectorId) : time;
    }
}
