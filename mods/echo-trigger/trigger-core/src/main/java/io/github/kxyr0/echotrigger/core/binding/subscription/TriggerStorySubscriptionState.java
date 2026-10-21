package io.github.kxyr0.echotrigger.core.binding.subscription;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionId;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TriggerStorySubscriptionState {
    private final TriggerSleepSubscriptionCalls sleepCalls = new TriggerSleepSubscriptionCalls();
    private final TriggerSeatingSubscriptionCalls seatingCalls = new TriggerSeatingSubscriptionCalls();
    private final TriggerVisibilitySubscriptionCalls visibilityCalls =
            new TriggerVisibilitySubscriptionCalls();
    private final TriggerTimeSubscriptionCalls timeCalls = new TriggerTimeSubscriptionCalls();
    private Optional<TriggerEventSelector> selector = Optional.empty();
    private Optional<TriggerSubscriptionTargetKind> targetKind = Optional.empty();
    private Optional<String> targetId = Optional.empty();

    public boolean has() {
        return selector.isPresent() && targetId.isPresent();
    }

    public Optional<TriggerStorySubscription> toSubscription(TriggerId triggerId, boolean fireOnlyOnce) {
        if (!has()) {
            return Optional.empty();
        }
        return Optional.of(new TriggerStorySubscription(
                new TriggerSubscriptionId(triggerId.value() + "#story"),
                triggerId,
                selector.orElseThrow(),
                targetKind.orElseThrow(),
                targetId.orElseThrow(),
                fireOnlyOnce));
    }

    public void readSubscribe(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        if (selector.isPresent()) {
            diagnostics.add(diagnostic("TriggerResource may declare only one Subscribe(...) selector."));
            return;
        }
        Optional<InvocationExpressionSyntax> selectorCall =
                io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax.positionalInvocation(call, 0);
        String qualified = selectorCall
                .map(io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax::qualifiedName)
                .orElse("");
        if (qualified.equals("Time.At")) {
            timeCalls.readSubscribe(call, diagnostics).ifPresent(value -> selector = Optional.of(value));
        } else if (qualified.startsWith("PlayerSleep.")) {
            sleepCalls.readSubscribe(call, aliases, diagnostics).ifPresent(value -> selector = Optional.of(value));
        } else if (visibilitySelector(qualified)) {
            visibilityCalls.readSubscribe(call, aliases, diagnostics)
                    .ifPresent(value -> selector = Optional.of(value));
        } else if (qualified.startsWith("Player.")) {
            seatingCalls.readSubscribe(call, aliases, diagnostics).ifPresent(value -> selector = Optional.of(value));
        } else {
            diagnostics.add(diagnostic("Subscribe(...) selector provider is not supported."));
        }
    }

    private static boolean visibilitySelector(String qualified) {
        return qualified.equals("Player.LostSightOf")
                || qualified.equals("Player.GainedSightOf")
                || qualified.equals("Player.TargetIsOutOfView");
    }

    public void readStartStory(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        readTarget(call, aliases, diagnostics, TriggerSubscriptionTargetKind.STORY,
                "StartStory requires a StoryId field or namespace:path string.");
    }

    public void readStartDialogue(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        readTarget(call, aliases, diagnostics, TriggerSubscriptionTargetKind.DIALOGUE,
                "StartDialogue requires a DialogueId field or namespace:path string.");
    }

    public void validate(List<TriggerDiagnostic> diagnostics) {
        if (selector.isPresent() != targetId.isPresent()) {
            diagnostics.add(diagnostic("Subscribe(...) and StartStory(...)/StartDialogue(...) "
                    + "must be declared together."));
        }
        if (targetKind.filter(TriggerSubscriptionTargetKind.DIALOGUE::equals).isPresent()
                && selector.filter(value -> !value.providerId().equals("echo_seating:events")
                        && !value.providerId().equals("echo_time:occurrences")).isPresent()) {
            diagnostics.add(diagnostic("StartDialogue supports Player.* seating or Time.At subscriptions."));
        }
    }

    private void readTarget(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics,
            TriggerSubscriptionTargetKind kind,
            String missingMessage) {
        if (targetId.isPresent()) {
            diagnostics.add(diagnostic("TriggerResource may declare only one StartStory(...) or StartDialogue(...)."));
            return;
        }
        Optional<String> value = resolveArgument(call, aliases);
        if (value.isEmpty()) {
            diagnostics.add(diagnostic(missingMessage));
            return;
        }
        targetKind = Optional.of(kind);
        targetId = value;
    }

    private static Optional<String> resolveArgument(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases) {
        return io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax.positionalString(call, 0)
                .or(() -> io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax
                        .positionalName(call, 0).flatMap(aliases));
    }

    private static TriggerDiagnostic diagnostic(String message) {
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID, message);
    }
}
