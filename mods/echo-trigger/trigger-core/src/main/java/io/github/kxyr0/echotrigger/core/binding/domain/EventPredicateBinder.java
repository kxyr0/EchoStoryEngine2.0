package io.github.kxyr0.echotrigger.core.binding.domain;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.subscription.EventFilter;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public final class EventPredicateBinder {
    public EventFilter bind(InvocationExpressionSyntax call, Function<String, Optional<String>> aliases) {
        String predicate = TriggerCallSyntax.qualifiedName(call);
        var kind = Arrays.stream(EventKind.values()).filter(value -> value.predicate().equals(predicate)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестное событие: " + predicate));
        requireArguments(call, kind);
        boolean animation = kind == EventKind.ANIMATION_MARKER || kind == EventKind.ANIMATION_COMPLETED;
        var resource = new ResourceId(argument(call, animation ? 1 : 0, aliases));
        Optional<SymbolId> subject = animation
                ? Optional.of(new SymbolId(new ResourceId(argument(call, 0, aliases)).value())) : Optional.empty();
        requireResource(kind, resource);
        return new EventFilter(kind, resource, detail(kind, call, aliases), subject);
    }
    private void requireArguments(InvocationExpressionSyntax call, EventKind kind) {
        int count = io.github.kxyr0.echoevents.api.catalog.EventCatalogDescriptor.catalog().stream()
                .filter(value -> value.kind() == kind).findFirst().orElseThrow().filterFields().size();
        if (call.arguments().size() != count || call.arguments().stream().anyMatch(value -> value.name().isPresent())) {
            throw new IllegalArgumentException(kind.predicate() + " требует " + count + " позиционных аргументов");
        }
    }
    private Optional<SymbolId> detail(EventKind kind, InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases) {
        return switch (kind) {
            case DIALOGUE_CHOICE -> Optional.of(new SymbolId(argument(call, 1, aliases)));
            case NPC_ARRIVED -> Optional.of(new SymbolId(new ResourceId(argument(call, 1, aliases)).value()));
            case ANIMATION_MARKER -> Optional.of(new SymbolId(argument(call, 2, aliases)));
            default -> Optional.empty();
        };
    }
    private void requireResource(EventKind kind, ResourceId resource) {
        if ((kind == EventKind.OBSERVATION_COMPLETED) != resource.value().contains("#")) {
            throw new IllegalArgumentException("#point требуется только для Observation.Completed");
        }
    }

    private String argument(
            InvocationExpressionSyntax call,
            int index,
            Function<String, Optional<String>> aliases) {
        return TriggerCallSyntax.positionalString(call, index)
                .or(() -> TriggerCallSyntax.positionalName(call, index).flatMap(aliases))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Требуется ResourceId или известный символ"));
    }
}
