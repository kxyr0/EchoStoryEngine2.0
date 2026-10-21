package io.github.kxyr0.echotrigger.core.binding.domain;

import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition.Location;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class EventTriggerBinder {
    public Optional<EventTriggerDefinition> bind(
            List<MethodSyntax> methods,
            TriggerId id,
            String revision,
            List<TriggerDiagnostic> diagnostics,
            Function<String, Optional<String>> aliases) {
        var init = methods.stream().filter(method -> method.name().equals("Init")).findFirst();
        if (init.isEmpty()) {
            return Optional.empty();
        }
        var calls =
                init.get().body().statements().stream()
                        .flatMap(value -> TriggerCallSyntax.statementCall(value).stream())
                        .toList();
        var when =
                calls.stream()
                        .filter(call -> TriggerCallSyntax.qualifiedName(call).equals("When"))
                        .toList();
        if (when.isEmpty()) {
            return Optional.empty();
        }
        try {
            validateInit(calls, when);
            return Optional.of(
                    definition(
                            calls,
                            methods,
                            when.get(0),
                            new BindingContext(id, revision, aliases)));
        } catch (IllegalArgumentException | java.util.NoSuchElementException failure) {
            diagnostics.add(
                    new TriggerDiagnostic(
                            TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID,
                            failure.getMessage(),
                            Optional.of(
                                    failure instanceof EventBindingException located
                                            ? located.source()
                                            : location(when.get(0).span()))));
            return Optional.empty();
        }
    }

    private void validateInit(
            List<InvocationExpressionSyntax> calls, List<InvocationExpressionSyntax> when) {
        if (when.size() != 1 || when.get(0).arguments().size() != 1) {
            throw new IllegalArgumentException(
                    "Требуется одна декларация When с одним typed predicate");
        }
        for (var call : calls) {
            String name = TriggerCallSyntax.qualifiedName(call);
            if (!name.equals("Id") && !name.equals("When") && !call.arguments().isEmpty()) {
                throw new EventBindingException("Once policy не принимает аргументы", location(call.span()));
            }
            if (!List.of("Id", "When", "Once", "FireOnlyOnce", "FireOnlyOncePerPlayer")
                    .contains(TriggerCallSyntax.qualifiedName(call))) {
                throw new IllegalArgumentException(
                        "Event subscription не поддерживает Init call: "
                                + TriggerCallSyntax.qualifiedName(call));
            }
        }
    }

    private EventTriggerDefinition definition(
            List<InvocationExpressionSyntax> calls,
            List<MethodSyntax> methods,
            InvocationExpressionSyntax when,
            BindingContext context) {
        var selector =
                TriggerCallSyntax.positionalInvocation(when, 0)
                        .orElseThrow(
                                () -> new IllegalArgumentException("When требует typed predicate"));
        io.github.kxyr0.echoevents.api.subscription.EventFilter filter;
        try {
            filter = new EventPredicateBinder().bind(selector, context.aliases());
        } catch (IllegalArgumentException failure) {
            throw new EventBindingException(failure.getMessage(), location(selector.span()));
        }
        boolean once =
                calls.stream()
                        .anyMatch(
                                call ->
                                        List.of("Once", "FireOnlyOnce", "FireOnlyOncePerPlayer")
                                                .contains(TriggerCallSyntax.qualifiedName(call)));
        var subscription =
                new EventSubscription(
                        new EventSubscriptionId(context.id().value() + "#When"),
                        filter,
                        context.revision(),
                        once);
        return new EventTriggerDefinition(
                subscription, location(selector.span()), actions(methods, context.aliases()));
    }

    private List<EventTriggerDefinition.Action> actions(
            List<MethodSyntax> methods, Function<String, Optional<String>> aliases) {
        var then = methods.stream().filter(method -> method.name().equals("Then")).toList();
        if (then.size() != 1 || !then.get(0).parameters().isEmpty()) {
            throw new IllegalArgumentException("Требуется Then()");
        }
        return new EventActionBinder().bind(then.get(0).body(), aliases);
    }

    private static Location location(SourceSpan span) {
        return new Location(span.sourceId().value(), span.start().line(), span.start().column());
    }

    private record BindingContext(
            TriggerId id, String revision, Function<String, Optional<String>> aliases) {}
}
