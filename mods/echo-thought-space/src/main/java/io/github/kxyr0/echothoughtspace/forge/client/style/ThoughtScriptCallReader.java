package io.github.kxyr0.echothoughtspace.forge.client.style;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ThoughtScriptCallReader {
    private ThoughtScriptCallReader() { }

    static String name(InvocationExpressionSyntax invocation) {
        if (invocation.target() instanceof NameExpressionSyntax name) return name.name();
        throw new IllegalArgumentException("Thought Space calls must use simple names.");
    }

    static Map<String, ExpressionSyntax> named(InvocationExpressionSyntax invocation) {
        Map<String, ExpressionSyntax> result = new LinkedHashMap<>();
        for (ArgumentSyntax argument : invocation.arguments()) {
            String name = argument.name().orElseThrow(
                    () -> new IllegalArgumentException(name(invocation) + " accepts named arguments only."));
            if (result.put(name, argument.value()) != null) {
                throw new IllegalArgumentException("Duplicate argument " + name + '.');
            }
        }
        return Map.copyOf(result);
    }

    static List<ExpressionSyntax> positional(InvocationExpressionSyntax invocation) {
        List<ExpressionSyntax> result = new ArrayList<>();
        for (ArgumentSyntax argument : invocation.arguments()) {
            if (argument.name().isPresent()) {
                throw new IllegalArgumentException(name(invocation) + " accepts positional arguments only.");
            }
            result.add(argument.value());
        }
        return List.copyOf(result);
    }

    static String string(ExpressionSyntax expression, String field) {
        if (expression instanceof StringLiteralExpressionSyntax text) return text.value();
        throw new IllegalArgumentException(field + " must be a string.");
    }

    static double number(ExpressionSyntax expression, String field) {
        if (expression instanceof DecimalLiteralExpressionSyntax decimal) return decimal.value().doubleValue();
        if (expression instanceof IntegerLiteralExpressionSyntax integer) return integer.value();
        throw new IllegalArgumentException(field + " must be a number.");
    }
}
