package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.value;

import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class IntrinsicValueReader {
    private final BiConsumer<String, Map<String, String>> report;

    public IntrinsicValueReader(BiConsumer<String, Map<String, String>> report) { this.report = report; }

    public Optional<String> string(ExpressionSyntax expression, String intrinsic, String argument) {
        if (expression instanceof StringLiteralExpressionSyntax value) {
            return Optional.of(value.value());
        }
        invalid(intrinsic, argument, "string");
        return Optional.empty();
    }

    public Optional<String> scalar(ExpressionSyntax expression, String intrinsic, String argument) {
        if (expression instanceof StringLiteralExpressionSyntax value) {
            return Optional.of(value.value());
        }
        if (expression instanceof BooleanLiteralExpressionSyntax value) {
            return Optional.of(Boolean.toString(value.value()));
        }
        if (expression instanceof IntegerLiteralExpressionSyntax value) {
            return Optional.of(Integer.toString(value.value()));
        }
        if (expression instanceof DecimalLiteralExpressionSyntax value) {
            return Optional.of(value.value().toPlainString());
        }
        if (ticks(expression)) {
            return ticksValue((InvocationExpressionSyntax) expression, intrinsic, argument);
        }
        invalid(intrinsic, argument, "literal");
        return Optional.empty();
    }

    public Optional<List<String>> vector(ExpressionSyntax expression, String intrinsic, String argument) {
        if (!(expression instanceof InvocationExpressionSyntax invocation) || invocation.arguments().size() != 3
                || !(invocation.target() instanceof NameExpressionSyntax name)
                || !("Vector3".equals(name.name()) || "Position".equals(name.name()))
                || invocation.arguments().stream().anyMatch(value -> value.name().isPresent())) {
            invalid(intrinsic, argument, "Position(x, y, z) or Vector3(x, y, z)");
            return Optional.empty();
        }
        List<String> coordinates = invocation.arguments().stream()
                .map(value -> scalar(value.value(), intrinsic, argument)).flatMap(Optional::stream).toList();
        return coordinates.size() == 3 ? Optional.of(List.copyOf(coordinates)) : Optional.empty();
    }

    private boolean ticks(ExpressionSyntax expression) {
        return expression instanceof InvocationExpressionSyntax invocation
                && invocation.target() instanceof NameExpressionSyntax name && "Ticks".equals(name.name());
    }

    private Optional<String> ticksValue(InvocationExpressionSyntax value, String intrinsic, String argument) {
        if (value.arguments().size() != 1 || value.arguments().get(0).name().isPresent()) {
            invalid(intrinsic, argument, "Ticks(literal)");
            return Optional.empty();
        }
        return scalar(value.arguments().get(0).value(), intrinsic, argument);
    }

    private void invalid(String intrinsic, String argument, String expected) {
        report.accept("ESE-SOURCE-024", Map.of(
                "intrinsic", intrinsic, "argument", argument, "expected", expected));
    }
}
