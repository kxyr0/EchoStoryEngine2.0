package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TriggerCallSyntax {
    private TriggerCallSyntax() {
    }

    public static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    public static String name(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (target instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    public static String qualifiedName(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        return name(invocation);
    }

    public static Optional<String> positionalString(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(TriggerCallSyntax::stringValue);
    }

    public static Optional<String> positionalName(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(TriggerCallSyntax::nameValue);
    }

    public static Optional<Integer> positionalInt(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(TriggerCallSyntax::intValue);
    }

    public static Optional<Double> positionalDouble(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).map(ArgumentSyntax::value).flatMap(TriggerCallSyntax::doubleValue);
    }

    public static Optional<ExpressionSyntax> positionalExpression(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).map(ArgumentSyntax::value);
    }

    public static Optional<InvocationExpressionSyntax> positionalInvocation(
            InvocationExpressionSyntax invocation,
            int index
    ) {
        return positional(invocation, index)
                .map(ArgumentSyntax::value)
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }

    public static Map<String, String> argumentValues(InvocationExpressionSyntax invocation) {
        Map<String, String> values = new LinkedHashMap<>();
        int positionalIndex = 0;
        for (ArgumentSyntax argument : invocation.arguments()) {
            Optional<String> value = textValue(argument.value());
            if (value.isEmpty()) {
                positionalIndex++;
                continue;
            }
            String key = argument.name().orElse("arg" + positionalIndex);
            values.put(key, value.orElseThrow());
            positionalIndex++;
        }
        return Map.copyOf(values);
    }

    public static Optional<String> namedText(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value)
                .flatMap(value -> textValue(value).stream())
                .findFirst();
    }

    public static Optional<ExpressionSyntax> namedExpression(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value)
                .findFirst();
    }

    public static String creationName(ObjectCreationExpressionSyntax creation) {
        return creation.type().name().text();
    }

    public static Optional<Double> doubleValue(ExpressionSyntax expression) {
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of((double) literal.value());
        }
        if (expression instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().doubleValue());
        }
        return Optional.empty();
    }

    private static Optional<ArgumentSyntax> positional(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return Optional.of(invocation.arguments().get(index));
    }

    private static Optional<String> stringValue(ArgumentSyntax argument) {
        if (argument.value() instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<String> nameValue(ArgumentSyntax argument) {
        if (argument.value() instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    private static Optional<Integer> intValue(ArgumentSyntax argument) {
        if (argument.value() instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<String> textValue(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(Integer.toString(literal.value()));
        }
        if (expression instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().stripTrailingZeros().toPlainString());
        }
        if (expression instanceof BooleanLiteralExpressionSyntax literal) {
            return Optional.of(Boolean.toString(literal.value()));
        }
        if (expression instanceof InvocationExpressionSyntax invocation) {
            return vectorText(invocation);
        }
        return Optional.empty();
    }

    private static Optional<String> vectorText(InvocationExpressionSyntax invocation) {
        String type = name(invocation);
        if (!(type.equals("Vector3") || type.equals("Position")) || invocation.arguments().size() != 3) {
            return Optional.empty();
        }
        Optional<String> x = textValue(invocation.arguments().get(0).value());
        Optional<String> y = textValue(invocation.arguments().get(1).value());
        Optional<String> z = textValue(invocation.arguments().get(2).value());
        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(type + "(" + x.orElseThrow() + "," + y.orElseThrow()
                        + "," + z.orElseThrow() + ")")
                : Optional.empty();
    }
}
