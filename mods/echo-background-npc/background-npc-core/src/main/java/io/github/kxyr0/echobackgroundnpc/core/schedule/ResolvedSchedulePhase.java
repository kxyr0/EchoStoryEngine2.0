package io.github.kxyr0.echobackgroundnpc.core.schedule;

import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.Objects;
import java.util.Optional;

public record ResolvedSchedulePhase(
        int index,
        BackgroundTask task,
        Optional<BackgroundScheduleEntry> entry
) {
    public ResolvedSchedulePhase {
        if (index < -1) throw new IllegalArgumentException("index cannot be below -1.");
        task = Objects.requireNonNull(task, "task");
        entry = Objects.requireNonNull(entry, "entry");
    }
}
