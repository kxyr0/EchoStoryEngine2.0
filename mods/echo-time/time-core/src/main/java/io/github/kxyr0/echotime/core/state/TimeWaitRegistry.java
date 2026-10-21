package io.github.kxyr0.echotime.core.state;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TimeWaitRegistry {
    private final Map<TimeWaitId, TimeWait> waits = new LinkedHashMap<>();

    public TimeWaitDecision register(TimeWaitId id, TimeOfDay requested, EchoInstant current) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(current, "current");
        TimeWait existing = waits.get(id);
        if (existing != null) {
            if (!existing.requestedTime().equals(requested)) {
                return new TimeWaitDecision(TimeWaitDecision.Status.IDEMPOTENCY_CONFLICT, existing);
            }
            return decision(existing, current);
        }
        long dayBase = Math.multiplyExact(current.day().value() - 1L, TimeOfDay.MICROS_PER_DAY);
        long todayTarget = Math.addExact(dayBase, requested.microseconds());
        long target = current.absoluteMicroseconds() <= todayTarget
                ? todayTarget : Math.addExact(todayTarget, TimeOfDay.MICROS_PER_DAY);
        TimeWait wait = new TimeWait(id, requested, target);
        waits.put(id, wait);
        return decision(wait, current);
    }

    public List<TimeWait> completed(EchoInstant current) {
        return waits.values().stream()
                .filter(wait -> current.absoluteMicroseconds() >= wait.targetAbsoluteMicroseconds())
                .toList();
    }

    public void remove(TimeWaitId id) {
        waits.remove(Objects.requireNonNull(id, "id"));
    }

    public List<TimeWait> waits() {
        return List.copyOf(waits.values());
    }

    public void restore(List<TimeWait> restored) {
        waits.clear();
        Objects.requireNonNull(restored, "restored").forEach(wait -> waits.put(wait.waitId(), wait));
    }

    private TimeWaitDecision decision(TimeWait wait, EchoInstant current) {
        TimeWaitDecision.Status status = current.absoluteMicroseconds() >= wait.targetAbsoluteMicroseconds()
                ? TimeWaitDecision.Status.COMPLETE : TimeWaitDecision.Status.WAITING;
        return new TimeWaitDecision(status, wait);
    }
}
