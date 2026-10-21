package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.expression.TriggerExpressionKind;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.model.TriggerExpression;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.vanilla.VanillaTriggerExpressions;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TriggerExpressionBinder {
    Optional<TriggerExpression> bind(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics
    ) {
        if (composite(call)) {
            return bindComposite(call, fields, diagnostics);
        }
        String name = TriggerCallSyntax.name(call);
        Optional<TriggerExpressionKind> expressionKind = VanillaTriggerExpressions.kind(name);
        Optional<TriggerArea> area = area(call, fields, name);
        if (expressionKind.isEmpty() || requiresArea(name) && area.isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.INVALID_EXPRESSION,
                    "Unsupported trigger expression: " + TriggerCallSyntax.name(call)));
            return Optional.empty();
        }
        Map<String, String> args = VanillaTriggerExpressions.normalizeArguments(name,
                TriggerCallSyntax.argumentValues(call));
        validate(name, args, diagnostics);
        TriggerEventKind kind = VanillaTriggerExpressions.eventKind(name, expressionKind.orElseThrow());
        TriggerCondition condition = TriggerCondition.expression(expressionKind.orElseThrow(), kind, area, name, args);
        return Optional.of(new TriggerExpression(source(call), condition));
    }

    boolean isEdgeEvent(InvocationExpressionSyntax call) {
        return VanillaTriggerExpressions.kind(TriggerCallSyntax.name(call))
                .filter(kind -> kind == TriggerExpressionKind.EVENT)
                .isPresent();
    }

    private Optional<TriggerExpression> bindComposite(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics
    ) {
        List<TriggerCondition> children = new ArrayList<>();
        for (ArgumentSyntax argument : call.arguments()) {
            if (argument.value() instanceof InvocationExpressionSyntax child) {
                bind(child, fields, diagnostics).map(TriggerExpression::condition).ifPresent(children::add);
            }
        }
        if (children.isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.INVALID_EXPRESSION, "Composite trigger expression is empty."));
            return Optional.empty();
        }
        return Optional.of(new TriggerExpression(source(call),
                TriggerCondition.composite(TriggerCallSyntax.name(call), children)));
    }

    private static Optional<TriggerArea> area(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            String name
    ) {
        Optional<InvocationExpressionSyntax> inline = TriggerCallSyntax.positionalInvocation(call, 0);
        if (requiresArea(name) && inline.isPresent() && TriggerCallSyntax.name(inline.orElseThrow()).equals("Area")) {
            return TriggerFieldCatalog.readAreaCall(inline.orElseThrow());
        }
        Optional<String> named = TriggerCallSyntax.namedText(call, "area");
        if (requiresArea(name)) {
            return named.flatMap(fields::area)
                    .or(() -> TriggerCallSyntax.positionalName(call, 0).flatMap(fields::area));
        }
        return named.flatMap(fields::area);
    }

    private static String source(InvocationExpressionSyntax call) {
        Optional<String> area = TriggerCallSyntax.positionalName(call, 0);
        return area.map(value -> TriggerCallSyntax.name(call) + "(" + value + ")")
                .orElseGet(() -> TriggerCallSyntax.name(call));
    }

    private static boolean composite(InvocationExpressionSyntax call) {
        String name = TriggerCallSyntax.name(call);
        return name.equals("Any") || name.equals("All") || name.equals("Not");
    }

    private static boolean requiresArea(String name) {
        return name.equals("PlayerEnter") || name.equals("PlayerExit")
                || name.equals("PlayerInside") || name.equals("PlayerStay");
    }

    private static void validate(String name, Map<String, String> args, List<TriggerDiagnostic> diagnostics) {
        if (args.containsKey("count") && parseLong(args.get("count")).filter(value -> value > 0).isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_ITEM_COUNT_INVALID, name + " count must be > 0."));
        }
        if (name.startsWith("Health") && parseDouble(args.get("value")).filter(value -> value >= 0).isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_HEALTH_VALUE_INVALID, name + " value must be finite and >= 0."));
        }
        if (name.startsWith("Food")
                && parseLong(args.get("value")).filter(value -> value >= 0 && value <= 20).isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_FOOD_VALUE_INVALID, name + " value must be in 0..20."));
        }
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Double> parseDouble(String value) {
        try {
            double parsed = value == null ? Double.NaN : Double.parseDouble(value);
            return Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
