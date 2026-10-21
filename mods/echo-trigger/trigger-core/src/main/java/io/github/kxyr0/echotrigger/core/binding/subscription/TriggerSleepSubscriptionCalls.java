package io.github.kxyr0.echotrigger.core.binding.subscription;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;

public final class TriggerSleepSubscriptionCalls {
    public Optional<TriggerEventSelector> readSubscribe(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> selectorCall = TriggerCallSyntax.positionalInvocation(call, 0);
        if (selectorCall.isEmpty()) {
            diagnostics.add(diagnostic("Subscribe requires PlayerSleep.Confirmed(Scenario)."));
            return Optional.empty();
        }
        return bindPlayerSleepSelector(selectorCall.orElseThrow(), aliases, diagnostics);
    }

    public Optional<String> readStartStory(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        Optional<String> storyId = resolveArgument(call, aliases);
        if (storyId.isEmpty()) {
            diagnostics.add(diagnostic("StartStory requires a StoryId field or namespace:path string."));
        }
        return storyId;
    }

    private Optional<TriggerEventSelector> bindPlayerSleepSelector(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        Optional<String> kind = sleepKind(TriggerCallSyntax.qualifiedName(call));
        Optional<String> scenario = resolveArgument(call, aliases);
        int diagnosticsBeforeCountValidation = diagnostics.size();
        OptionalLong sleepCount = positiveLong(call, "sleepCount", diagnostics);
        OptionalLong sleepCountAtLeast = positiveLong(call, "sleepCountAtLeast", diagnostics);
        if (sleepCount.isPresent() && sleepCountAtLeast.isPresent()) {
            diagnostics.add(new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID,
                    "PlayerSleep selector may use sleepCount or sleepCountAtLeast, not both."));
            return Optional.empty();
        }
        if (diagnostics.size() > diagnosticsBeforeCountValidation) {
            return Optional.empty();
        }
        if (kind.isEmpty() || scenario.isEmpty()) {
            diagnostics.add(new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID,
                    "Subscribe supports PlayerSleep.Confirmed/EnteredDream/Returned/Completed/Failed."));
            return Optional.empty();
        }
        return Optional.of(TriggerEventSelector.of("echo_player_sleep:events", kind.orElseThrow(),
                scenario.orElseThrow(), sleepCount, sleepCountAtLeast));
    }

    private static Optional<String> resolveArgument(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases) {
        return TriggerCallSyntax.positionalString(call, 0)
                .or(() -> TriggerCallSyntax.positionalName(call, 0).flatMap(aliases));
    }

    private static Optional<String> sleepKind(String qualified) {
        return switch (qualified) {
            case "PlayerSleep.Confirmed" -> Optional.of("CONFIRMED");
            case "PlayerSleep.EnteredDream" -> Optional.of("ENTERED_DREAM");
            case "PlayerSleep.Returned" -> Optional.of("RETURNED");
            case "PlayerSleep.Completed" -> Optional.of("COMPLETED");
            case "PlayerSleep.Failed" -> Optional.of("FAILED");
            default -> Optional.empty();
        };
    }

    private static OptionalLong positiveLong(
            InvocationExpressionSyntax call,
            String name,
            List<TriggerDiagnostic> diagnostics) {
        Optional<String> raw = TriggerCallSyntax.namedText(call, name);
        if (raw.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            long parsed = Long.parseLong(raw.orElseThrow());
            if (parsed <= 0L) {
                diagnostics.add(new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID,
                        name + " must be positive."));
                return OptionalLong.empty();
            }
            return OptionalLong.of(parsed);
        } catch (NumberFormatException exception) {
            diagnostics.add(new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID,
                    name + " must be a positive integer."));
            return OptionalLong.empty();
        }
    }

    private static TriggerDiagnostic diagnostic(String message) {
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID, message);
    }
}
