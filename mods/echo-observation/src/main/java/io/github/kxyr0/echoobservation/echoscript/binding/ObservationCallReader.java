package io.github.kxyr0.echoobservation.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class ObservationCallReader {
    private ObservationCallReader() {
    }

    static String name(InvocationExpressionSyntax call) {
        return expressionName(call.target());
    }

    static Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        List<ArgumentSyntax> values = call.arguments().stream().filter(value -> value.name().isEmpty()).toList();
        return index < values.size() ? Optional.of(values.get(index).value()) : Optional.empty();
    }

    static Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(value -> value.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).findFirst();
    }

    static Optional<String> string(ExpressionSyntax expression) {
        return expression instanceof StringLiteralExpressionSyntax value ? Optional.of(value.value())
                : Optional.empty();
    }

    static Optional<Double> number(ExpressionSyntax expression) {
        if (expression instanceof DecimalLiteralExpressionSyntax value) {
            return Optional.of(value.value().doubleValue());
        }
        if (expression instanceof IntegerLiteralExpressionSyntax value) {
            return Optional.of((double) value.value());
        }
        return Optional.empty();
    }

    static Optional<Integer> integer(ExpressionSyntax expression) {
        return expression instanceof IntegerLiteralExpressionSyntax value
                ? Optional.of(value.value()) : Optional.empty();
    }

    static Optional<Boolean> bool(ExpressionSyntax expression) {
        return expression instanceof BooleanLiteralExpressionSyntax value ? Optional.of(value.value())
                : Optional.empty();
    }

    static Optional<String> scalar(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax value) return Optional.of(value.value());
        if (expression instanceof IntegerLiteralExpressionSyntax value) return Optional.of(Integer.toString(value.value()));
        if (expression instanceof DecimalLiteralExpressionSyntax value) return Optional.of(value.value().toString());
        if (expression instanceof BooleanLiteralExpressionSyntax value) return Optional.of(Boolean.toString(value.value()));
        String name = expressionName(expression);
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    static Optional<InvocationExpressionSyntax> invocation(ExpressionSyntax expression, String name) {
        return expression instanceof InvocationExpressionSyntax call && name(call).equals(name)
                ? Optional.of(call) : Optional.empty();
    }

    private static String expressionName(ExpressionSyntax expression) {
        if (expression instanceof NameExpressionSyntax name) return name.name();
        if (expression instanceof MemberAccessExpressionSyntax member) {
            String target = expressionName(member.target());
            return target.isBlank() ? member.memberName() : target + "." + member.memberName();
        }
        return "";
    }
}
