package io.github.kxyr0.echoanimation.echoscript.procedural;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.Optional;

/**
 * Typed syntax reads shared by the procedural animation binders.
 */
public final class ProceduralSyntax {
    private ProceduralSyntax() {
    }

    public static ExpressionSyntax argument(InvocationExpressionSyntax call, int index, String name) {
        Optional<ArgumentSyntax> named = name == null ? Optional.empty() : call.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent()).findFirst();
        if (named.isPresent()) {
            return named.orElseThrow().value();
        }
        if (index >= 0 && index < call.arguments().size()) {
            return call.arguments().get(index).value();
        }
        throw new IllegalArgumentException("Missing argument " + (name == null ? index : name) + ".");
    }

    public static Optional<ExpressionSyntax> optional(InvocationExpressionSyntax call, int index, String name) {
        try {
            return Optional.of(argument(call, index, name));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static String string(InvocationExpressionSyntax call, int index, String name) {
        ExpressionSyntax value = argument(call, index, name);
        if (value instanceof StringLiteralExpressionSyntax string) {
            return string.value();
        }
        throw new IllegalArgumentException("Expected a string for " + field(index, name) + ".");
    }

    public static int integer(InvocationExpressionSyntax call, int index, String name) {
        ExpressionSyntax value = argument(call, index, name);
        if (value instanceof IntegerLiteralExpressionSyntax integer) {
            return integer.value();
        }
        throw new IllegalArgumentException("Expected an integer for " + field(index, name) + ".");
    }

    public static double number(InvocationExpressionSyntax call, int index, String name) {
        return number(argument(call, index, name));
    }

    public static double number(ExpressionSyntax value) {
        if (value instanceof IntegerLiteralExpressionSyntax integer) {
            return integer.value();
        }
        if (value instanceof DecimalLiteralExpressionSyntax decimal) {
            return decimal.value().doubleValue();
        }
        throw new IllegalArgumentException("Expected a numeric expression.");
    }

    public static String name(ExpressionSyntax expression) {
        if (expression instanceof NameExpressionSyntax name) {
            return name.name();
        }
        throw new IllegalArgumentException("Expected a local variable or symbolic value.");
    }

    public static InvocationExpressionSyntax invocation(ExpressionSyntax expression) {
        if (expression instanceof InvocationExpressionSyntax call) {
            return call;
        }
        throw new IllegalArgumentException("Expected an intrinsic call.");
    }

    public static Optional<String> owner(InvocationExpressionSyntax call) {
        if (call.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    private static String field(int index, String name) {
        return name == null ? "argument " + index : "field '" + name + "'";
    }
    public static String enumSymbol(InvocationExpressionSyntax call, int index, String name) {
        String value = ProceduralSyntax.name(ProceduralSyntax.argument(call, index, name));
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
    }

    public static int optionalInteger( InvocationExpressionSyntax call, int index, String name, int fallback) {
        return ProceduralSyntax.optional(call, index, name) .map(value -> ProceduralSyntax.integer(call, index, name))
                .orElse(fallback);
    }
}
