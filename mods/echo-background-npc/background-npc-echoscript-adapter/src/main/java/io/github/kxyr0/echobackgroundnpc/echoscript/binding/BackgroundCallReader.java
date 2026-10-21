package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.List;
import java.util.Optional;

final class BackgroundCallReader {
    String name(InvocationExpressionSyntax call) {
        return call.target() instanceof NameExpressionSyntax name ? name.name() : "";
    }
    Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        return call.arguments().stream().filter(value -> value.name().isEmpty()).skip(index).findFirst()
                .map(ArgumentSyntax::value);
    }
    Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(value -> value.name().filter(name::equals).isPresent()).findFirst()
                .map(ArgumentSyntax::value);
    }
    Optional<String> string(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }
    Optional<String> namedString(InvocationExpressionSyntax call, String name) {
        return named(call, name).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }
    Optional<String> symbol(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(NameExpressionSyntax.class::isInstance)
                .map(NameExpressionSyntax.class::cast).map(NameExpressionSyntax::name);
    }
    Optional<Double> number(InvocationExpressionSyntax call, int index) {
        return positional(call, index).flatMap(this::number);
    }
    Optional<Double> namedNumber(InvocationExpressionSyntax call, String name) {
        return named(call, name).flatMap(this::number);
    }
    Optional<String> nestedVoice(InvocationExpressionSyntax call) {
        return named(call, "voice").filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast).filter(value -> name(value).equals("Voice"))
                .flatMap(value -> string(value, 0));
    }
    List<InvocationExpressionSyntax> calls(ClassSyntax resource, String name) {
        return resource.members().stream().filter(ClassCallSyntax.class::isInstance).map(ClassCallSyntax.class::cast)
                .map(ClassCallSyntax::invocation).filter(value -> name(value).equals(name)).toList();
    }
    List<ClassSectionSyntax> sections(ClassSyntax resource, String name) {
        return resource.members().stream().filter(ClassSectionSyntax.class::isInstance)
                .map(ClassSectionSyntax.class::cast).filter(value -> name(value.invocation()).equals(name)).toList();
    }
    Optional<InvocationExpressionSyntax> statement(ExpressionStatementSyntax statement) {
        return statement.expression() instanceof InvocationExpressionSyntax call ? Optional.of(call) : Optional.empty();
    }
    BackgroundSourceRef source(SourceSpan span) {
        return new BackgroundSourceRef(span.sourceId().value(), span.start().line(), span.start().column());
    }
    private Optional<Double> number(ExpressionSyntax expression) {
        if (expression instanceof DecimalLiteralExpressionSyntax value) return Optional.of(value.value().doubleValue());
        if (expression instanceof IntegerLiteralExpressionSyntax value) return Optional.of((double) value.value());
        return Optional.empty();
    }
}
