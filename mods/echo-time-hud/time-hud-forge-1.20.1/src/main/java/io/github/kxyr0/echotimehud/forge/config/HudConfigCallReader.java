package io.github.kxyr0.echotimehud.forge.config;

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

final class HudConfigCallReader {
    Optional<InvocationExpressionSyntax> call(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax value
                && value.expression() instanceof InvocationExpressionSyntax invocation) return Optional.of(invocation);
        return Optional.empty();
    }

    String name(InvocationExpressionSyntax invocation) {
        if (invocation.target() instanceof NameExpressionSyntax value) return value.name();
        if (invocation.target() instanceof MemberAccessExpressionSyntax value) return value.memberName();
        return "";
    }

    Optional<String> string(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }

    Optional<String> symbol(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(NameExpressionSyntax.class::isInstance)
                .map(NameExpressionSyntax.class::cast).map(NameExpressionSyntax::name);
    }

    Optional<Boolean> bool(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(BooleanLiteralExpressionSyntax.class::isInstance)
                .map(BooleanLiteralExpressionSyntax.class::cast).map(BooleanLiteralExpressionSyntax::value);
    }

    Optional<BigDecimal> number(InvocationExpressionSyntax call, int index) {
        return positional(call, index).flatMap(this::number);
    }

    Optional<BigDecimal> number(InvocationExpressionSyntax call, String name) {
        return named(call, name).flatMap(this::number);
    }

    private Optional<BigDecimal> number(ExpressionSyntax value) {
        if (value instanceof DecimalLiteralExpressionSyntax decimal) return Optional.of(decimal.value());
        if (value instanceof IntegerLiteralExpressionSyntax integer) return Optional.of(BigDecimal.valueOf(integer.value()));
        return Optional.empty();
    }

    private Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        if (index < 0 || index >= call.arguments().size()) return Optional.empty();
        ArgumentSyntax value = call.arguments().get(index);
        return value.name().isEmpty() ? Optional.of(value.value()) : Optional.empty();
    }

    private Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(value -> value.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).findFirst();
    }
}

