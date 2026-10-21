package io.github.kxyr0.echotrigger.core.binding.subscription;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TriggerVisibilitySubscriptionCalls {
    private static final String PROVIDER = "echo_visibility:events";

    public Optional<TriggerEventSelector> readSubscribe(
            InvocationExpressionSyntax subscribe,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> selector = TriggerCallSyntax.positionalInvocation(subscribe, 0);
        if (selector.isEmpty()) {
            diagnostics.add(diagnostic("Subscribe requires a Player visibility selector."));
            return Optional.empty();
        }
        InvocationExpressionSyntax call = selector.orElseThrow();
        Optional<String> kind = kind(TriggerCallSyntax.qualifiedName(call));
        Optional<String> watchId = TriggerCallSyntax.positionalString(call, 0)
                .or(() -> TriggerCallSyntax.positionalName(call, 0).flatMap(aliases));
        if (kind.isEmpty() || watchId.isEmpty() || call.arguments().size() != 1) {
            diagnostics.add(diagnostic("Subscribe supports Player.LostSightOf(watch), "
                    + "Player.GainedSightOf(watch), or Player.TargetIsOutOfView(watch)."));
            return Optional.empty();
        }
        try {
            return Optional.of(TriggerEventSelector.of(PROVIDER, kind.orElseThrow(), watchId.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(diagnostic(exception.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<String> kind(String qualified) {
        return switch (qualified) {
            case "Player.LostSightOf" -> Optional.of("LOST_SIGHT");
            case "Player.GainedSightOf" -> Optional.of("GAINED_SIGHT");
            case "Player.TargetIsOutOfView" -> Optional.of("TARGET_IS_OUT_OF_VIEW");
            default -> Optional.empty();
        };
    }

    private static TriggerDiagnostic diagnostic(String message) {
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID, message);
    }
}
