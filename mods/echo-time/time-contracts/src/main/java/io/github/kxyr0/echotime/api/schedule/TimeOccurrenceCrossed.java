package io.github.kxyr0.echotime.api.schedule;

import io.github.kxyr0.echotime.api.identity.TimeOccurrenceId;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.Objects;

public record TimeOccurrenceCrossed(
        TimeOccurrenceId occurrenceId,
        TimeScheduleEntry entry,
        EchoInstant occurrence,
        EchoInstant intervalStart,
        EchoInstant intervalEnd,
        TimeCrossingReason reason) {
    public TimeOccurrenceCrossed {
        occurrenceId = Objects.requireNonNull(occurrenceId, "occurrenceId");
        entry = Objects.requireNonNull(entry, "entry");
        occurrence = Objects.requireNonNull(occurrence, "occurrence");
        intervalStart = Objects.requireNonNull(intervalStart, "intervalStart");
        intervalEnd = Objects.requireNonNull(intervalEnd, "intervalEnd");
        reason = Objects.requireNonNull(reason, "reason");
    }
}
