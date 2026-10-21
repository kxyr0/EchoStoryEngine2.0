package io.github.kxyr0.echocontentdoctor.validation.events;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echocontentdoctor.validation.reference.ReferenceValidator;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EventSubscriptionValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (var resource : graph.snapshot().resources()) {
            for (var event : resource.eventSubscriptions()) {
                if (!identities.add(event.subscription().id().value())) diagnostics.add(problem(resource, event,
                        "ECHO_DOCTOR_EVENT_SUBSCRIPTION_DUPLICATE", "Повторяющаяся подписка.", List.of()));
                validateSource(graph, resource, event, diagnostics);
                validateActions(graph, resource, event, diagnostics);
            }
        }
        diagnostics.addAll(new EventCycleValidator().validate(graph));
        return diagnostics;
    }
    private void validateSource(ContentGraph graph, ContentDescriptor owner, EventTriggerDefinition event,
            List<ContentDiagnostic> diagnostics) {
        var filter = event.subscription().filter();
        var kind = sourceKind(filter.kind());
        var id = new ContentResourceId(filter.resource().value().split("#", 2)[0]);
        var sources = graph.resolve(kind.targetKind(), id);
        if (sources.isEmpty()) {
            var reference = new ContentReference(kind, id, span(event.matcherSource()));
            diagnostics.add(problem(owner, event, "ECHO_DOCTOR_EVENT_SOURCE_NOT_FOUND", kind + " \"" + id.value()
                    + "\" не найден.", new ReferenceValidator().suggestions(graph, reference)));
        } else if (filter.kind() == EventKind.DIALOGUE_CHOICE) {
            String choice = filter.detail().orElseThrow().value();
            boolean found = sources.stream().flatMap(value -> value.nodes().stream()).anyMatch(node -> node.symbols().contains(choice));
            if (!found) diagnostics.add(problem(owner, event, "ECHO_DOCTOR_EVENT_CHOICE_NOT_FOUND",
                    "ChoiceId \"" + choice + "\" не найден в " + id.value(), List.of()));
        } else if (filter.kind() == EventKind.OBSERVATION_COMPLETED) {
            String[] parts = filter.resource().value().split("#", 2);
            if (parts.length != 2 || sources.stream().flatMap(value -> value.nodes().stream())
                    .noneMatch(node -> node.id().equals(parts[1]))) diagnostics.add(problem(owner, event,
                    "ECHO_DOCTOR_EVENT_OBSERVATION_NOT_FOUND", "Observation point не найден: " + filter.resource().value(), List.of()));
        }
        if (filter.kind() == EventKind.NPC_ARRIVED) {
            var target = new ContentResourceId(filter.detail().orElseThrow().value());
            if (graph.resolve(ContentResourceKind.LOCATION, target).isEmpty()) diagnostics.add(problem(owner, event,
                    "ECHO_DOCTOR_EVENT_LOCATION_NOT_FOUND", "LocationId не найден: " + target.value(), List.of()));
        }
        filter.subject().ifPresent(subject -> {
            var actor = new ContentResourceId(subject.value());
            if (graph.resolve(ContentResourceKind.NPC, actor).isEmpty() && graph.resolve(ContentResourceKind.PLAYER, actor).isEmpty()) {
                diagnostics.add(problem(owner, event, "ECHO_DOCTOR_EVENT_SUBJECT_NOT_FOUND", "Subject не найден: " + actor.value(), List.of()));
            }
        });
    }
    private void validateActions(ContentGraph graph, ContentDescriptor owner, EventTriggerDefinition event,
            List<ContentDiagnostic> diagnostics) {
        for (var action : event.actions()) {
            ContentResourceKind kind = switch (action.kind()) {
                case SCENE_PLAY -> ContentResourceKind.SCENE;
                case STORY_START -> ContentResourceKind.STORY;
                case DIALOGUE_PLAY -> ContentResourceKind.DIALOGUE;
                case OBSERVATION_ACTIVATE, OBSERVATION_ENABLE -> ContentResourceKind.OBSERVATION;
            };
            var target = new ContentResourceId(action.target().value().split("#", 2)[0]);
            if (graph.resolve(kind, target).isEmpty()) diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR,
                    "ECHO_DOCTOR_EVENT_ACTION_TARGET_NOT_FOUND", owner.id(), span(action.source()),
                    kind + " не найден: " + target.value(), List.of()));
            if (action.kind() == EventTriggerDefinition.Kind.OBSERVATION_ENABLE) {
                String point = action.target().value().split("#", 2)[1];
                if (graph.resolve(kind, target).stream().flatMap(value -> value.nodes().stream())
                        .noneMatch(value -> value.id().equals(point))) diagnostics.add(new ContentDiagnostic(
                        ContentSeverity.ERROR, "ECHO_DOCTOR_EVENT_ACTION_POINT_NOT_FOUND", owner.id(),
                        span(action.source()), "Observation point не найден: " + action.target().value(), List.of()));
            }
            if (event.subscription().filter().kind() == EventKind.NPC_DIED) diagnostics.add(new ContentDiagnostic(
                    ContentSeverity.ERROR, "ECHO_DOCTOR_EVENT_CONTEXT_REQUIRED", owner.id(), span(action.source()),
                    "World event не содержит current player для этой action.", List.of()));
        }
    }
    static ContentReferenceKind sourceKind(EventKind kind) {
        return switch (kind) {
            case DIALOGUE_STARTED, DIALOGUE_CHOICE, DIALOGUE_COMPLETED, DIALOGUE_CANCELLED, DIALOGUE_FAILED -> ContentReferenceKind.DIALOGUE;
            case SCENE_STARTED, SCENE_COMPLETED, SCENE_CANCELLED, SCENE_FAILED -> ContentReferenceKind.SCENE;
            case OBSERVATION_COMPLETED -> ContentReferenceKind.OBSERVATION;
            case NPC_ARRIVED, NPC_DIED -> ContentReferenceKind.NPC;
            case ANIMATION_MARKER, ANIMATION_COMPLETED -> ContentReferenceKind.ANIMATION;
            case CUTSCENE_STARTED, CUTSCENE_COMPLETED, CUTSCENE_CANCELLED -> ContentReferenceKind.CUTSCENE;
        };
    }
    static io.github.kxyr0.echoscript.api.source.SourceSpan span(EventTriggerDefinition.Location location) {
        var position = new io.github.kxyr0.echoscript.api.source.SourcePosition(0, location.line(), location.column());
        return new io.github.kxyr0.echoscript.api.source.SourceSpan(new io.github.kxyr0.echoscript.api.source.SourceId(location.sourceId()),
                0, 0, position, position);
    }
    private ContentDiagnostic problem(ContentDescriptor owner, EventTriggerDefinition event, String code,
            String message, List<ContentResourceId> suggestions) {
        return new ContentDiagnostic(ContentSeverity.ERROR, code, owner.id(), span(event.matcherSource()), message, suggestions);
    }
}
