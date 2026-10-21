package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Optional;

/**
 * Typed reads for supported Echo Animation intrinsic arguments.
 */
public final class ArgumentReader {
    public Optional<String> string(InvocationExpressionSyntax call, int index, String name) {
        return expression(call, index, name).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }

    public int integer(InvocationExpressionSyntax call, int index, String name, int fallback) {
        return expression(call, index, name).filter(IntegerLiteralExpressionSyntax.class::isInstance)
                .map(IntegerLiteralExpressionSyntax.class::cast).map(IntegerLiteralExpressionSyntax::value)
                .orElse(fallback);
    }

    public Optional<Double> authoredNumber(InvocationExpressionSyntax call, int index, String name) {
        return number(call, index, name, true);
    }

    public Optional<Double> rawNumber(InvocationExpressionSyntax call, int index, String name) {
        return number(call, index, name, false);
    }

    public double rawNumber(InvocationExpressionSyntax call, int index, String name, double fallback) {
        return rawNumber(call, index, name).orElse(fallback);
    }

    private Optional<Double> number(InvocationExpressionSyntax call, int index, String name, boolean integerMilli) {
        Optional<ExpressionSyntax> value = expression(call, index, name);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        ExpressionSyntax expression = value.orElseThrow();
        if (expression instanceof IntegerLiteralExpressionSyntax integer) {
            return Optional.of(integerMilli ? milli(integer.value()) : checked(integer.value(), name));
        }
        if (expression instanceof DecimalLiteralExpressionSyntax decimal) {
            return Optional.of(checked(decimal.value().doubleValue(), name));
        }
        if (expression instanceof StringLiteralExpressionSyntax string) {
            return Optional.of(decimal(string.value(), name));
        }
        throw new IllegalArgumentException(fieldName(name) + " must be integer-milli or decimal string.");
    }

    public double authoredNumber(InvocationExpressionSyntax call, int index, String name, int fallbackMilli) {
        return authoredNumber(call, index, name).orElse(milli(fallbackMilli));
    }

    public boolean bool(InvocationExpressionSyntax call, String name, boolean fallback) {
        return expression(call, -1, name).filter(BooleanLiteralExpressionSyntax.class::isInstance)
                .map(BooleanLiteralExpressionSyntax.class::cast).map(BooleanLiteralExpressionSyntax::value)
                .orElse(fallback);
    }

    public List<InvocationExpressionSyntax> calls(InvocationExpressionSyntax call, String name) {
        return expressions(call, name).stream()
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast)
                .toList();
    }

    public List<ExpressionSyntax> expressions(InvocationExpressionSyntax call, String name) {
        return expression(call, -1, name)
                .filter(ArrayLiteralExpressionSyntax.class::isInstance)
                .map(ArrayLiteralExpressionSyntax.class::cast)
                .map(ArrayLiteralExpressionSyntax::elements)
                .orElse(List.of());
    }

    public Optional<InvocationExpressionSyntax> call(InvocationExpressionSyntax call, String name) {
        return expression(call, -1, name)
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }

    public boolean has(InvocationExpressionSyntax call, String name) {
        return expression(call, -1, name).isPresent();
    }

    public void requireKnown(
            InvocationExpressionSyntax call,
            List<String> names,
            List<AnimationDiagnostic> diagnostics
    ) {
        for (ArgumentSyntax argument : call.arguments()) {
            if (argument.name().isPresent() && !names.contains(argument.name().orElseThrow())) {
                diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-UNSUPPORTED-FIELD",
                        "Unsupported field: " + argument.name().orElseThrow(), argument.span()));
            }
        }
    }

    public double milli(int value) {
        return value / 1000.0D;
    }

    private double decimal(String text, String name) {
        try {
            return checked(Double.parseDouble(text.trim()), name);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName(name) + " must be decimal string: " + text);
        }
    }

    private double checked(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName(name) + " must be finite.");
        }
        return value;
    }

    private String fieldName(String name) {
        return name == null ? "Positional argument" : "Field " + name;
    }

    private Optional<ExpressionSyntax> expression(InvocationExpressionSyntax call, int index, String name) {
        if (name != null) {
            return call.arguments().stream()
                    .filter(argument -> argument.name().filter(name::equals).isPresent())
                    .findFirst()
                    .map(ArgumentSyntax::value);
        }
        if (index >= 0 && index < call.arguments().size()) {
            return Optional.of(call.arguments().get(index).value());
        }
        return Optional.empty();
    }
}
