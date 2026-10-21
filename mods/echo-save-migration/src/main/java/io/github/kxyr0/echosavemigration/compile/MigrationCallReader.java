package io.github.kxyr0.echosavemigration.compile;

import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echosavemigration.api.StateValue;
import java.util.List;
import java.util.Optional;

final class MigrationCallReader {
    Optional<Call> read(ClassCallSyntax syntax) {
        InvocationExpressionSyntax invocation = syntax.invocation();
        if (!(invocation.target() instanceof NameExpressionSyntax name)) {
            return Optional.empty();
        }
        return Optional.of(new Call(name.name(), invocation.arguments().stream()
                .map(argument -> argument.value()).toList(), syntax.span()));
    }

    String string(Call call, int index) {
        ExpressionSyntax value = call.arguments().get(index);
        if (value instanceof StringLiteralExpressionSyntax literal) return literal.value();
        if (value instanceof NameExpressionSyntax name) return name.name();
        throw new IllegalArgumentException("Expected string argument.");
    }

    int integer(Call call, int index) {
        ExpressionSyntax value = call.arguments().get(index);
        if (value instanceof IntegerLiteralExpressionSyntax integer) return integer.value();
        throw new IllegalArgumentException("Expected integer argument.");
    }

    StateValue state(Call call, int index) {
        ExpressionSyntax value = call.arguments().get(index);
        if (value instanceof IntegerLiteralExpressionSyntax integer) {
            return new StateValue.IntState(integer.value());
        }
        if (value instanceof BooleanLiteralExpressionSyntax bool) return new StateValue.BoolState(bool.value());
        if (value instanceof StringLiteralExpressionSyntax text) return new StateValue.StringState(text.value());
        throw new IllegalArgumentException("Expected typed scalar state value.");
    }

    record Call(String name, List<ExpressionSyntax> arguments,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) {
        Call { arguments = List.copyOf(arguments); }
    }
}
