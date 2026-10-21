package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record ScheduleFact(String actorId, String time, ScheduleCategory category, String target, SourceSpan span) {
    public ScheduleFact {
        actorId = Objects.requireNonNull(actorId, "actorId");
        time = Objects.requireNonNull(time, "time");
        category = Objects.requireNonNull(category, "category");
        target = Objects.requireNonNull(target, "target");
        span = Objects.requireNonNull(span, "span");
    }
}
