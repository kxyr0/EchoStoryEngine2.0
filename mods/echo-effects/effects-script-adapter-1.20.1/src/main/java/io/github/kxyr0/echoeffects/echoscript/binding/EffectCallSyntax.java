package io.github.kxyr0.echoeffects.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.Optional;

public final class EffectCallSyntax {
    private EffectCallSyntax() {
    }

    public static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    public static String qualifiedName(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        if (target instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (target instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    public static Optional<String> positionalString(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return stringValue(invocation.arguments().get(index));
    }

    public static Optional<Double> positionalNumber(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return numberValue(invocation.arguments().get(index));
    }

    public static Optional<Double> namedNumber(InvocationExpressionSyntax invocation, String name) {
        return namedArgument(invocation, name).flatMap(EffectCallSyntax::numberValue);
    }

    public static Optional<String> namedString(InvocationExpressionSyntax invocation, String name) {
        return namedArgument(invocation, name).flatMap(EffectCallSyntax::stringValue);
    }

    public static Optional<Boolean> namedBoolean(InvocationExpressionSyntax invocation, String name) {
        return namedArgument(invocation, name).flatMap(EffectCallSyntax::booleanValue);
    }

    public static Optional<InvocationExpressionSyntax> positionalInvocation(
            InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return invocationValue(invocation.arguments().get(index));
    }

    public static Optional<InvocationExpressionSyntax> namedInvocation(
            InvocationExpressionSyntax invocation, String name) {
        return namedArgument(invocation, name).flatMap(EffectCallSyntax::invocationValue);
    }

    public static Optional<String> nameValue(ArgumentSyntax argument) {
        if (argument.value() instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    private static Optional<ArgumentSyntax> namedArgument(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst();
    }

    private static Optional<String> stringValue(ArgumentSyntax argument) {
        if (argument.value() instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<Double> numberValue(ArgumentSyntax argument) {
        if (argument.value() instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of((double) literal.value());
        }
        if (argument.value() instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().doubleValue());
        }
        return Optional.empty();
    }

    private static Optional<Boolean> booleanValue(ArgumentSyntax argument) {
        if (argument.value() instanceof BooleanLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<InvocationExpressionSyntax> invocationValue(ArgumentSyntax argument) {
        if (argument.value() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }
}
