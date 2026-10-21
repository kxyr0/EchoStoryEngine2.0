package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.Optional;

final class SpellSyntax {
    private SpellSyntax() { }

    static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) return Optional.of(invocation);
        return Optional.empty();
    }

    static String name(InvocationExpressionSyntax call) {
        ExpressionSyntax target = call.target();
        if (target instanceof NameExpressionSyntax simple) return simple.name();
        if (target instanceof MemberAccessExpressionSyntax access) {
            if (access.target() instanceof NameExpressionSyntax qualifier) return qualifier.name() + "." + access.memberName();
            return access.memberName();
        }
        return "";
    }

    static Optional<ArgumentSyntax> argument(InvocationExpressionSyntax call, String name, int position) {
        Optional<ArgumentSyntax> named = call.arguments().stream()
                .filter(value -> value.name().filter(name::equals).isPresent()).findFirst();
        if (named.isPresent()) return named;
        return position >= 0 && position < call.arguments().size() ? Optional.of(call.arguments().get(position)) : Optional.empty();
    }
}

