package io.github.kxyr0.echodevscenarios.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class ScenarioCallReader {
    Optional<Call> read(ExpressionSyntax expression) {
        if (!(expression instanceof InvocationExpressionSyntax invocation)
                || !(invocation.target() instanceof MemberAccessExpressionSyntax access)
                || !(access.target() instanceof NameExpressionSyntax service)) return Optional.empty();
        List<Value> values = invocation.arguments().stream().map(argument -> value(argument.value())).toList();
        return Optional.of(new Call(service.name(), access.memberName(), values, invocation.span()));
    }

    String classCall(InvocationExpressionSyntax invocation) {
        return invocation.arguments().stream().findFirst().map(argument -> value(argument.value()).text())
                .orElseThrow(() -> new IllegalArgumentException("Expected class-call argument."));
    }

    private Value value(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax text) return new Value(text.value(), true, text.span());
        if (expression instanceof IntegerLiteralExpressionSyntax integer) {
            return new Value(Integer.toString(integer.value()), true, integer.span());
        }
        if (expression instanceof NameExpressionSyntax name) return new Value(name.name(), false, name.span());
        if (expression instanceof InvocationExpressionSyntax invocation) {
            String target = invocation.target() instanceof NameExpressionSyntax name ? name.name() : "value";
            String args = invocation.arguments().stream().map(argument -> value(argument.value()).text())
                    .reduce((left, right) -> left + "," + right).orElse("");
            return new Value(target + "(" + args + ")", true, invocation.span());
        }
        return new Value(expression.toString(), true, expression.span());
    }

    record Call(String service, String operation, List<Value> arguments,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) { }
    record Value(String text, boolean literal, io.github.kxyr0.echoscript.api.source.SourceSpan span) { }
}
