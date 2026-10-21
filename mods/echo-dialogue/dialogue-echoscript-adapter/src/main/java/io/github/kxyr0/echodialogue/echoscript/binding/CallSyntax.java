package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Small syntax helpers for reading intrinsic-like calls without executing source.
 */
public final class CallSyntax {
    private CallSyntax() {
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

    public static String rootName(InvocationExpressionSyntax invocation) {
        List<InvocationSegment> segments = chain(invocation);
        return segments.isEmpty() ? qualifiedName(invocation) : segments.get(0).name();
    }

    public static List<InvocationSegment> chain(InvocationExpressionSyntax invocation) {
        List<InvocationSegment> segments = new ArrayList<>();
        InvocationExpressionSyntax current = invocation;
        while (true) {
            String name = name(current);
            if (name.isEmpty()) {
                return List.of();
            }
            segments.add(new InvocationSegment(name, current));
            if (!(current.target() instanceof MemberAccessExpressionSyntax access)) {
                break;
            }
            if (!(access.target() instanceof InvocationExpressionSyntax previous)) {
                return List.of();
            }
            current = previous;
        }
        Collections.reverse(segments);
        return List.copyOf(segments);
    }

    public static Optional<String> positionalString(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return stringValue(invocation.arguments().get(index));
    }

    public static Optional<String> namedString(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst()
                .flatMap(CallSyntax::stringValue);
    }

    public static Optional<Boolean> namedBoolean(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst()
                .flatMap(CallSyntax::booleanValue);
    }

    public static Optional<String> positionalName(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return nameValue(invocation.arguments().get(index));
    }

    public static Optional<String> positionalSimpleValue(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return simpleValue(invocation.arguments().get(index).value());
    }

    public static long namedArgumentCount(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .count();
    }

    public static Optional<InvocationExpressionSyntax> namedInvocation(InvocationExpressionSyntax invocation,
            String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst()
                .flatMap(CallSyntax::invocationValue);
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

    public static Optional<String> simpleValue(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof BooleanLiteralExpressionSyntax literal) {
            return Optional.of(Boolean.toString(literal.value()));
        }
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(Integer.toString(literal.value()));
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
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

    public record InvocationSegment(String name, InvocationExpressionSyntax invocation) {
    }
}
