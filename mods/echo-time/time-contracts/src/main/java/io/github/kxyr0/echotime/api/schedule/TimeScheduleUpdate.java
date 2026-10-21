package io.github.kxyr0.echotime.api.schedule;

import java.util.List;
import java.util.Objects;

public record TimeScheduleUpdate(String providerId, long catalogRevision, List<TimeScheduleEntry> entries) {
    public TimeScheduleUpdate {
        providerId = Objects.requireNonNull(providerId, "providerId").trim();
        if (providerId.isEmpty()) throw new IllegalArgumentException("providerId cannot be blank.");
        if (catalogRevision < 0L) throw new IllegalArgumentException("catalogRevision cannot be negative.");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
}
