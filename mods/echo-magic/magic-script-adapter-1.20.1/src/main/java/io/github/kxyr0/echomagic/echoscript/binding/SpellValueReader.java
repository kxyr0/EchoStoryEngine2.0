package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.Optional;

final class SpellValueReader {
    private final SpellConstants constants;

    SpellValueReader(SpellConstants constants) { this.constants = constants; }

    String string(InvocationExpressionSyntax call, String name, int position) {
        return string(argument(call, name, position).value());
    }
    double number(InvocationExpressionSyntax call, String name, int position) {
        return number(argument(call, name, position).value());
    }
    boolean bool(InvocationExpressionSyntax call, String name, int position) {
        return bool(argument(call, name, position).value());
    }
    String enumName(InvocationExpressionSyntax call, String name, int position) {
        ExpressionSyntax value = argument(call, name, position).value();
        if (value instanceof NameExpressionSyntax literal) return constants.string(literal.name()).orElse(literal.name());
        throw new IllegalArgumentException("Expected enum value for " + name + '.');
    }
    InvocationExpressionSyntax invocation(InvocationExpressionSyntax call, String name, int position) {
        ExpressionSyntax value = argument(call, name, position).value();
        if (value instanceof InvocationExpressionSyntax invocation) return invocation;
        throw new IllegalArgumentException("Expected invocation for " + name + '.');
    }
    Optional<InvocationExpressionSyntax> optionalInvocation(InvocationExpressionSyntax call, String name,
                                                             int position) {
        return SpellSyntax.argument(call, name, position).map(ArgumentSyntax::value).map(value -> {
            if (value instanceof InvocationExpressionSyntax invocation) return invocation;
            throw new IllegalArgumentException("Expected invocation for " + name + '.');
        });
    }

    private ArgumentSyntax argument(InvocationExpressionSyntax call, String name, int position) {
        return SpellSyntax.argument(call, name, position)
                .orElseThrow(() -> new IllegalArgumentException("Missing argument: " + name));
    }
    private String string(ExpressionSyntax value) {
        if (value instanceof StringLiteralExpressionSyntax literal) return literal.value();
        if (value instanceof NameExpressionSyntax name) return constants.string(name.name()).orElse(name.name());
        throw new IllegalArgumentException("Expected string value.");
    }
    private double number(ExpressionSyntax value) {
        if (value instanceof IntegerLiteralExpressionSyntax literal) return literal.value();
        if (value instanceof DecimalLiteralExpressionSyntax literal) return literal.value().doubleValue();
        if (value instanceof NameExpressionSyntax name) return constants.number(name.name()).orElseThrow();
        throw new IllegalArgumentException("Expected number value.");
    }
    private boolean bool(ExpressionSyntax value) {
        if (value instanceof BooleanLiteralExpressionSyntax literal) return literal.value();
        if (value instanceof NameExpressionSyntax name) return constants.bool(name.name()).orElseThrow();
        throw new IllegalArgumentException("Expected boolean value.");
    }
}
