package io.github.kxyr0.echotime.forge.config;

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
import java.math.BigDecimal;
import java.util.Optional;

final class TimeConfigCallReader {
    Optional<InvocationExpressionSyntax> call(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax value
                && value.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    String name(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof NameExpressionSyntax value) return value.name();
        if (target instanceof MemberAccessExpressionSyntax value) return value.memberName();
        return "";
    }

    Optional<String> string(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }

    Optional<Integer> integer(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(IntegerLiteralExpressionSyntax.class::isInstance)
                .map(IntegerLiteralExpressionSyntax.class::cast).map(IntegerLiteralExpressionSyntax::value);
    }

    Optional<Boolean> bool(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(BooleanLiteralExpressionSyntax.class::isInstance)
                .map(BooleanLiteralExpressionSyntax.class::cast).map(BooleanLiteralExpressionSyntax::value);
    }

    Optional<BigDecimal> decimal(InvocationExpressionSyntax call, String name) {
        return named(call, name).flatMap(this::decimal);
    }

    private Optional<BigDecimal> decimal(ExpressionSyntax expression) {
        if (expression instanceof DecimalLiteralExpressionSyntax value) return Optional.of(value.value());
        if (expression instanceof IntegerLiteralExpressionSyntax value) return Optional.of(BigDecimal.valueOf(value.value()));
        return Optional.empty();
    }

    private Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        if (index < 0 || index >= call.arguments().size()) return Optional.empty();
        ArgumentSyntax argument = call.arguments().get(index);
        return argument.name().isEmpty() ? Optional.of(argument.value()) : Optional.empty();
    }

    private Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(argument -> argument.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).findFirst();
    }
}
