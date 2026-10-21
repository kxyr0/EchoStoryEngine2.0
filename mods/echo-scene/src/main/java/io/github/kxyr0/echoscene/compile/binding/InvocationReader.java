package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class InvocationReader {
    Optional<BoundCall> read(ExpressionSyntax expression) {
        if (!(expression instanceof InvocationExpressionSyntax invocation)
                || !(invocation.target() instanceof MemberAccessExpressionSyntax access)
                || !(access.target() instanceof NameExpressionSyntax receiver)) {
            return Optional.empty();
        }
        List<BoundCall.BoundValue> values = new ArrayList<>();
        for (ArgumentSyntax argument : invocation.arguments()) {
            Optional<BoundCall.BoundValue> value = value(argument.value());
            if (value.isEmpty()) {
                return Optional.empty();
            }
            values.add(value.orElseThrow());
        }
        return Optional.of(new BoundCall(receiver.name(), access.memberName(), values, invocation.span()));
    }

    private Optional<BoundCall.BoundValue> value(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(new BoundCall.BoundValue(literal.value(), true, literal.span()));
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(new BoundCall.BoundValue(name.name(), false, name.span()));
        }
        if (expression instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(new BoundCall.BoundValue(render(invocation), false, invocation.span()));
        }
        return Optional.of(new BoundCall.BoundValue(expression.toString(), true, expression.span()));
    }

    private String render(InvocationExpressionSyntax invocation) {
        String target = invocation.target() instanceof NameExpressionSyntax name ? name.name() : "value";
        return target + "(" + invocation.arguments().stream()
                .map(argument -> value(argument.value()).orElseThrow().value()).reduce((a, b) -> a + "," + b)
                .orElse("") + ")";
    }
}
