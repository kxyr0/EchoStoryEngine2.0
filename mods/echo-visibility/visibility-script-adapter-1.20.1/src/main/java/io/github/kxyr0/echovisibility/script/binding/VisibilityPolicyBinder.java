package io.github.kxyr0.echovisibility.script.binding;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityEvaluationRate;
import io.github.kxyr0.echovisibility.api.policy.VisibilityOcclusionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class VisibilityPolicyBinder {
    Optional<VisibilityViewpoint> viewpoint(InvocationExpressionSyntax call, List<VisibilityDiagnostic> diagnostics) {
        return enumValue(call, VisibilityViewpoint.class, diagnostics, "Viewpoint");
    }

    Optional<VisibilityDetectionMode> detection(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        return enumValue(call, VisibilityDetectionMode.class, diagnostics, "HiddenWhen");
    }

    Optional<VisibilityOcclusionMode> occlusion(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        return enumValue(call, VisibilityOcclusionMode.class, diagnostics, "Occlusion");
    }

    Optional<VisibilityConcealmentPolicy> concealment(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        return enumValue(call, VisibilityConcealmentPolicy.class, diagnostics, "Concealment");
    }

    Optional<VisibilityEvaluationRate> evaluation(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        return enumValue(call, VisibilityEvaluationRate.class, diagnostics, "Evaluate");
    }

    List<VisibilitySampleKind> samples(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        List<VisibilitySampleKind> values = new ArrayList<>();
        for (var argument : call.arguments()) {
            VisibilityCallSyntax.text(argument.value()).flatMap(value -> parse(value, VisibilitySampleKind.class))
                    .ifPresentOrElse(values::add, () -> invalid(diagnostics, "Unknown Samples(...) value."));
        }
        if (values.isEmpty()) {
            invalid(diagnostics, "Samples(...) must contain at least one sample kind.");
        }
        return List.copyOf(values);
    }

    Optional<Integer> wrappedInt(
            InvocationExpressionSyntax call,
            String wrapper,
            List<VisibilityDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> valueCall = VisibilityCallSyntax.positionalCall(call, 0);
        Optional<Double> number = valueCall.filter(value -> VisibilityCallSyntax.name(value).equals(wrapper))
                .flatMap(value -> VisibilityCallSyntax.positionalDouble(value, 0));
        if (number.isEmpty() || number.orElseThrow() != Math.rint(number.orElseThrow())) {
            invalid(diagnostics, VisibilityCallSyntax.name(call) + " requires " + wrapper + "(integer).");
            return Optional.empty();
        }
        return Optional.of(number.orElseThrow().intValue());
    }

    private static <T extends Enum<T>> Optional<T> enumValue(
            InvocationExpressionSyntax call,
            Class<T> type,
            List<VisibilityDiagnostic> diagnostics,
            String label) {
        Optional<String> name = VisibilityCallSyntax.positionalText(call, 0);
        Optional<T> value = name.flatMap(text -> parse(text, type));
        if (value.isEmpty()) {
            invalid(diagnostics, label + " contains an unsupported typed value.");
        }
        return value;
    }

    private static <T extends Enum<T>> Optional<T> parse(String value, Class<T> type) {
        try {
            return Optional.of(Enum.valueOf(type, value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void invalid(List<VisibilityDiagnostic> diagnostics, String message) {
        diagnostics.add(VisibilityDiagnostic.of(VisibilityDiagnosticCode.VISIBILITY_WATCH_INVALID, message));
    }
}
