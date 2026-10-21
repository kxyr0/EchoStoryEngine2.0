package io.github.kxyr0.echodialogue.echoscript.binding.command.core;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ArgumentValues {
    private ArgumentValues() {
    }

    public static Map<String, String> read(InvocationExpressionSyntax invocation) {
        Map<String, String> values = new LinkedHashMap<>();
        int positionalIndex = 0;
        for (ArgumentSyntax argument : invocation.arguments()) {
            int index = positionalIndex;
            simple(argument.value())
                    .ifPresent(value -> values.put(argument.name().orElse("arg" + index), value));
            positionalIndex++;
        }
        return Map.copyOf(values);
    }

    public static void copy(Map<String, String> values, String source, String target) {
        if (!values.containsKey(target) && values.containsKey(source)) {
            values.put(target, values.get(source));
        }
    }

    public static void removePositionals(Map<String, String> values) {
        values.keySet().removeIf(key -> key.startsWith("arg"));
    }

    public static void remove(Map<String, String> values, String... keys) {
        for (String key : keys) {
            values.remove(key);
        }
    }

    public static boolean copyInlineVector(
            InvocationExpressionSyntax call,
            Map<String, String> values) {
        if (call.arguments().size() < 2) {
            return false;
        }
        ExpressionSyntax expression = call.arguments().get(1).value();
        List<ArgumentSyntax> coordinates;
        if (expression instanceof InvocationExpressionSyntax vector
                && vector.target() instanceof NameExpressionSyntax name
                && "Vector3".equals(name.name())) {
            coordinates = vector.arguments();
        } else if (expression instanceof ObjectCreationExpressionSyntax vector
                && "Vector3".equals(vector.type().name().text())) {
            coordinates = vector.arguments();
        } else {
            return false;
        }
        if (coordinates.size() != 3) {
            return false;
        }
        simple(coordinates.get(0).value()).ifPresent(value -> values.put("x", value));
        simple(coordinates.get(1).value()).ifPresent(value -> values.put("y", value));
        simple(coordinates.get(2).value()).ifPresent(value -> values.put("z", value));
        return values.containsKey("x") && values.containsKey("y") && values.containsKey("z");
    }

    public static void copyBlockPosition(
            Map<String, String> values,
            String key,
            Function<String, Optional<Map<String, String>>> resolver) {
        if (values.containsKey("x") || !values.containsKey(key)) {
            return;
        }
        resolver.apply(values.get(key)).ifPresent(values::putAll);
    }

    public static void copyInlineBlockPosition(
            InvocationExpressionSyntax call,
            Map<String, String> values) {
        if (values.containsKey("x") || call.arguments().size() < 2) {
            return;
        }
        ExpressionSyntax expression = call.arguments().get(1).value();
        if (!(expression instanceof ObjectCreationExpressionSyntax creation)
                || !"BlockPosition".equals(creation.type().name().text())
                || creation.arguments().size() != 3) {
            return;
        }
        simple(creation.arguments().get(0).value()).ifPresent(value -> values.put("x", value));
        simple(creation.arguments().get(1).value()).ifPresent(value -> values.put("y", value));
        simple(creation.arguments().get(2).value()).ifPresent(value -> values.put("z", value));
    }

    public static Optional<String> simple(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof BooleanLiteralExpressionSyntax literal) {
            return Optional.of(Boolean.toString(literal.value()));
        }
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(Integer.toString(literal.value()));
        }
        if (expression instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().toPlainString());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }
}
