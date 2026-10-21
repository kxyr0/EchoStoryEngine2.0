package io.github.kxyr0.echotrigger.core.binding.policy;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.policy.TriggerStayPolicy;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

public final class TriggerInitPolicyReader {
    public Optional<TriggerScope> readScope(
            InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        Optional<TriggerScope> value = TriggerCallSyntax.positionalName(call, 0)
                .flatMap(name -> enumValue(TriggerScope.class, name));
        if (value.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.INVALID_SCOPE,
                    "Scope must be GLOBAL, SERVER, PLAYER, or DIALOGUE_SESSION."));
        }
        return value;
    }

    public Optional<TriggerMode> readMode(
            InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        Optional<TriggerMode> value = TriggerCallSyntax.positionalName(call, 0)
                .flatMap(name -> enumValue(TriggerMode.class, name));
        if (value.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.INVALID_DEFINITION, "Mode must be ONCE or REPEAT."));
        }
        return value;
    }

    public Optional<Long> readTicks(InvocationExpressionSyntax call) {
        return TriggerCallSyntax.positionalInvocation(call, 0)
                .filter(inner -> TriggerCallSyntax.name(inner).equals("Ticks"))
                .flatMap(inner -> TriggerCallSyntax.positionalInt(inner, 0))
                .or(() -> TriggerCallSyntax.positionalInt(call, 0))
                .map(Integer::longValue);
    }

    public Optional<Integer> readFireCount(
            InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        Optional<Integer> count = TriggerCallSyntax.positionalInt(call, 0).filter(value -> value > 0);
        if (count.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_FIRE_POLICY_CONFLICT,
                    "FireCount requires a positive integer."));
        }
        return count;
    }

    public Optional<Long> readStayInterval(
            InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        Optional<Long> ticks = readTicks(call).filter(value -> value > 0);
        if (ticks.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_STAY_INTERVAL_INVALID,
                    "StayInterval requires positive ticks."));
        }
        return ticks;
    }

    public Optional<TriggerStayPolicy> readStayPolicy(
            InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        Optional<TriggerStayPolicy> value = TriggerCallSyntax.positionalName(call, 0)
                .flatMap(name -> enumValue(TriggerStayPolicy.class, name));
        if (value.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_STAY_POLICY_INVALID,
                    "StayPolicy must be SKIP_WHILE_RUNNING or QUEUE_ONE."));
        }
        return value;
    }

    private static <E extends Enum<E>> Optional<E> enumValue(Class<E> type, String value) {
        try {
            return Optional.of(Enum.valueOf(type, value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static TriggerDiagnostic diagnostic(TriggerDiagnosticCode code, String message) {
        return new TriggerDiagnostic(code, message);
    }
}
