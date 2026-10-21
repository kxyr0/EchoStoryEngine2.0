package io.github.kxyr0.echocontentdoctor.validation.events;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EventCycleValidator {
    List<ContentDiagnostic> validate(ContentGraph graph) {
        Map<String, List<String>> edges = new HashMap<>();
        for (var owner : graph.snapshot().resources()) for (var event : owner.eventSubscriptions()) {
            String source = key(event.subscription().filter().kind(), event.subscription().filter().resource().value());
            if (!completion(event.subscription().filter().kind()) || event.subscription().once()) continue;
            for (var action : event.actions()) {
                var kind = switch (action.kind()) {
                    case SCENE_PLAY -> EventKind.SCENE_COMPLETED;
                    case DIALOGUE_PLAY -> EventKind.DIALOGUE_COMPLETED;
                    default -> null;
                };
                if (kind != null) edges.computeIfAbsent(source, ignored -> new ArrayList<>())
                        .add(key(kind, action.target().value()));
            }
        }
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (var owner : graph.snapshot().resources()) for (var event : owner.eventSubscriptions()) {
            String source = key(event.subscription().filter().kind(), event.subscription().filter().resource().value());
            if (!completion(event.subscription().filter().kind()) || event.subscription().once()) continue;
            if (reaches(source, edges)) diagnostics.add(new ContentDiagnostic(
                    ContentSeverity.WARNING, "ECHO_DOCTOR_EVENT_CYCLE", owner.id(), EventSubscriptionValidator.span(event.matcherSource()),
                    "Возможен цикл completion → запуск ресурса → completion.", List.of()));
        }
        return diagnostics;
    }
    private boolean completion(EventKind kind) {
        return kind == EventKind.SCENE_COMPLETED || kind == EventKind.DIALOGUE_COMPLETED;
    }
    private String key(EventKind kind, String resource) { return kind.stableId() + "|" + resource; }
    private boolean reaches(String target, Map<String, List<String>> edges) {
        var visited = new HashSet<String>();
        var pending = new java.util.ArrayDeque<>(edges.getOrDefault(target, List.of()));
        while (!pending.isEmpty()) {
            String next = pending.removeFirst();
            if (next.equals(target)) return true;
            if (visited.add(next)) pending.addAll(edges.getOrDefault(next, List.of()));
        }
        return false;
    }
}
