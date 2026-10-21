package io.github.kxyr0.echotrigger.core.binding.subscription;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TriggerSeatingSubscriptionCalls {
    public Optional<TriggerEventSelector> readSubscribe(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> selectorCall = TriggerCallSyntax.positionalInvocation(call, 0);
        if (selectorCall.isEmpty()) {
            diagnostics.add(diagnostic("Subscribe requires Player.SeatedIn/SeatedAt/StoodUpIn/StoodUpFrom."));
            return Optional.empty();
        }
        return bindSelector(selectorCall.orElseThrow(), aliases, diagnostics);
    }

    private Optional<TriggerEventSelector> bindSelector(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases,
            List<TriggerDiagnostic> diagnostics) {
        String qualified = TriggerCallSyntax.qualifiedName(call);
        Optional<String> kind = seatingKind(qualified);
        SelectorArguments arguments = arguments(call, qualified, aliases);
        if (kind.isEmpty() || invalidSelector(qualified, arguments)) {
            diagnostics.add(diagnostic("Subscribe supports Player.SeatedIn(zone), Player.SeatedAt(slot), "
                    + "Player.SeatedAt(zone, slot), Player.StoodUpIn(zone), Player.StoodUpFrom(slot), "
                    + "Player.StoodUpFrom(zone, slot), Player.ChangedSeatIn(zone)."));
            return Optional.empty();
        }
        if (slotSelector(qualified) && arguments.zone().isEmpty()) {
            return Optional.of(TriggerEventSelector.seatingAt("echo_seating:events",
                    kind.orElseThrow(), arguments.slot().orElseThrow()));
        }
        return Optional.of(TriggerEventSelector.seating("echo_seating:events",
                kind.orElseThrow(), arguments.zone().orElseThrow(), arguments.slot()));
    }

    private SelectorArguments arguments(
            InvocationExpressionSyntax call,
            String qualified,
            Function<String, Optional<String>> aliases) {
        Optional<String> first = resolveArgument(call, 0, aliases);
        Optional<String> second = resolveArgument(call, 1, aliases);
        if (slotSelector(qualified)) {
            return second.isPresent()
                    ? new SelectorArguments(first, second)
                    : new SelectorArguments(Optional.empty(), first);
        }
        return new SelectorArguments(first, second);
    }

    private static Optional<String> resolveArgument(
            InvocationExpressionSyntax call,
            int index,
            Function<String, Optional<String>> aliases) {
        return TriggerCallSyntax.positionalString(call, index)
                .or(() -> TriggerCallSyntax.positionalName(call, index).flatMap(aliases));
    }

    private static Optional<String> seatingKind(String qualified) {
        return switch (qualified) {
            case "Player.SeatedIn", "Player.SeatedAt" -> Optional.of("SEATED");
            case "Player.StoodUpIn", "Player.StoodUpFrom" -> Optional.of("STOOD_UP");
            case "Player.ChangedSeatIn" -> Optional.of("CHANGED_SEAT");
            default -> Optional.empty();
        };
    }

    private static boolean invalidSelector(String qualified, SelectorArguments arguments) {
        return switch (qualified) {
            case "Player.SeatedAt", "Player.StoodUpFrom" -> arguments.slot().isEmpty();
            case "Player.SeatedIn", "Player.StoodUpIn", "Player.ChangedSeatIn" ->
                    arguments.zone().isEmpty() || arguments.slot().isPresent();
            default -> true;
        };
    }

    private static boolean slotSelector(String qualified) {
        return List.of("Player.SeatedAt", "Player.StoodUpFrom").contains(qualified);
    }

    private static TriggerDiagnostic diagnostic(String message) {
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_SEATING_SELECTOR_INVALID, message);
    }

    private record SelectorArguments(Optional<String> zone, Optional<String> slot) {
    }
}
