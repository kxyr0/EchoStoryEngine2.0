package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict literal argument boundary; unknown and repeated named arguments never silently pass. */
final class InspectionArguments {
    private InspectionArguments() { }

    static String name(InvocationExpressionSyntax call) {
        if (call.target() instanceof NameExpressionSyntax name) return name.name();
        throw failure("Only unqualified inspection declarations are supported.", call);
    }

    static List<ArgumentSyntax> ordered(InvocationExpressionSyntax call, String... names) {
        List<ArgumentSyntax> args = call.arguments();
        if (args.size() != names.length) throw failure(name(call) + " expects " + names.length + " arguments.", call);
        if (args.stream().allMatch(value -> value.name().isEmpty())) return args;
        Set<String> seen = new HashSet<>();
        Set<String> allowed = Set.of(names);
        for (ArgumentSyntax argument : args) {
            String name = argument.name().orElse("");
            if (!allowed.contains(name) || !seen.add(name)) {
                throw failure("Unknown, mixed, or repeated named argument: " + name, call);
            }
        }
        return java.util.Arrays.stream(names).map(name -> args.stream()
                .filter(value -> value.name().orElse("").equals(name)).findFirst().orElseThrow()).toList();
    }

    static String item(InvocationExpressionSyntax call) {
        ArgumentSyntax argument = ordered(call, "id").get(0);
        if (argument.value() instanceof StringLiteralExpressionSyntax string
                && string.value().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return string.value();
        throw failure("Item expects one canonical namespaced item ID string.", call);
    }

    static String text(InvocationExpressionSyntax call, int maximum) {
        if (call == null) return "";
        ArgumentSyntax argument = ordered(call, "value").get(0);
        if (argument.value() instanceof StringLiteralExpressionSyntax string
                && string.value().codePointCount(0, string.value().length()) <= maximum) return string.value();
        throw new BindingFailure("ITEM_INSPECTION_INVALID_TEXT", name(call) + " expects a string literal of at most "
                + maximum + " characters.", argument.span());
    }

    static double number(ArgumentSyntax argument, InspectionSource source) {
        ExpressionSyntax expression = argument.value();
        if (!(expression instanceof IntegerLiteralExpressionSyntax)
                && !(expression instanceof DecimalLiteralExpressionSyntax)) {
            throw new BindingFailure("ITEM_INSPECTION_INVALID_VIEW", "Expected a finite numeric literal.", argument.span());
        }
        String literal = source.text().substring(expression.span().startOffset(), expression.span().endOffset());
        double value = new BigDecimal(literal).doubleValue();
        if (!Double.isFinite(value)) {
            throw new BindingFailure("ITEM_INSPECTION_INVALID_VIEW", "Numeric value must be finite.", argument.span());
        }
        return value;
    }

    static BindingFailure failure(String message, InvocationExpressionSyntax call) {
        return new BindingFailure("ITEM_INSPECTION_INVALID_DECLARATION", message, call.span());
    }
}
