package io.github.kxyr0.echotrigger.core.binding.subscription;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

public final class TriggerTimeSubscriptionCalls {
    public Optional<TriggerEventSelector> readSubscribe(
            InvocationExpressionSyntax call, List<TriggerDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> selector = TriggerCallSyntax.positionalInvocation(call, 0);
        if (selector.isEmpty() || !"Time.At".equals(TriggerCallSyntax.qualifiedName(selector.orElseThrow()))) {
            return Optional.empty();
        }
        InvocationExpressionSyntax at = selector.orElseThrow();
        if (at.arguments().size() == 1) {
            return daily(TriggerCallSyntax.positionalString(at, 0), diagnostics);
        }
        if (at.arguments().size() == 2) {
            return specific(TriggerCallSyntax.positionalInvocation(at, 0),
                    TriggerCallSyntax.positionalString(at, 1), diagnostics);
        }
        diagnostics.add(diagnostic("Time.At requires HH:mm or Day(n), HH:mm."));
        return Optional.empty();
    }

    private Optional<TriggerEventSelector> daily(
            Optional<String> time, List<TriggerDiagnostic> diagnostics) {
        if (!validTime(time)) {
            diagnostics.add(diagnostic("Time.At requires strict 24-hour HH:mm."));
            return Optional.empty();
        }
        return Optional.of(TriggerEventSelector.of("echo_time:occurrences", "AT",
                "echo_time:daily/" + safe(time.orElseThrow())));
    }

    private Optional<TriggerEventSelector> specific(
            Optional<InvocationExpressionSyntax> dayCall, Optional<String> time,
            List<TriggerDiagnostic> diagnostics) {
        Optional<Integer> day = dayCall.filter(call -> "Day".equals(TriggerCallSyntax.name(call)))
                .flatMap(call -> TriggerCallSyntax.positionalInt(call, 0));
        if (day.filter(value -> value > 0).isEmpty() || !validTime(time)) {
            diagnostics.add(diagnostic("Time.At(Day(n), HH:mm) requires positive day and strict time."));
            return Optional.empty();
        }
        return Optional.of(TriggerEventSelector.of("echo_time:occurrences", "AT",
                "echo_time:day/" + day.orElseThrow() + "/" + safe(time.orElseThrow())));
    }

    private boolean validTime(Optional<String> value) {
        return value.filter(time -> time.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")).isPresent();
    }

    private String safe(String time) { return time.replace(':', '_'); }

    private TriggerDiagnostic diagnostic(String message) {
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID, message);
    }
}
