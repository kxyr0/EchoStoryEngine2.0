package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.fact.ScheduleCategory;
import io.github.kxyr0.echocontentdoctor.fact.ScheduleFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScheduleValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            Map<String, List<ScheduleFact>> slots = new HashMap<>();
            descriptor.schedules().stream().filter(value -> value.category() == ScheduleCategory.STATEFUL_EXCLUSIVE)
                    .forEach(value -> slots.computeIfAbsent(value.actorId() + "@" + value.time(),
                            ignored -> new ArrayList<>()).add(value));
            slots.values().stream().filter(values -> values.stream().map(ScheduleFact::target).distinct().count() > 1)
                    .flatMap(List::stream).forEach(value -> diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR,
                            "ECHO_DOCTOR_SCHEDULE_CONFLICT", descriptor.id(), value.span(),
                            "Exclusive schedule conflict for " + value.actorId() + " at " + value.time(), List.of())));
        }
        return diagnostics;
    }
}
