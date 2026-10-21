package io.github.kxyr0.echoquests.echoscript.binding;

import io.github.kxyr0.echoquests.api.source.QuestSourceRef;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
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
import java.util.List;
import java.util.Optional;

final class QuestCallReader {
    String name(InvocationExpressionSyntax call) {
        if (call.target() instanceof NameExpressionSyntax name) return name.name();
        if (call.target() instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        return "";
    }

    Optional<String> string(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }

    int positionalCount(InvocationExpressionSyntax call) {
        return (int) call.arguments().stream().filter(value -> value.name().isEmpty()).count();
    }

    List<ExpressionSyntax> positionals(InvocationExpressionSyntax call) {
        return call.arguments().stream().filter(value -> value.name().isEmpty()).map(ArgumentSyntax::value).toList();
    }

    Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(value -> value.name().filter(name::equals).isPresent()).findFirst()
                .map(ArgumentSyntax::value);
    }

    Optional<String> namedString(InvocationExpressionSyntax call, String name) {
        return named(call, name).filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast).map(StringLiteralExpressionSyntax::value);
    }

    Optional<String> namedScalar(InvocationExpressionSyntax call, String name) {
        return named(call, name).flatMap(this::scalar);
    }

    List<String> namedArguments(InvocationExpressionSyntax call) {
        return call.arguments().stream().flatMap(value -> value.name().stream()).toList();
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

    QuestSourceRef source(SourceSpan span) {
        return new QuestSourceRef(span.sourceId().value(), span.start().line(), span.start().column());
    }

    private Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        return call.arguments().stream().filter(value -> value.name().isEmpty()).skip(index).findFirst()
                .map(ArgumentSyntax::value);
    }

    private Optional<String> scalar(ExpressionSyntax value) {
        if (value instanceof StringLiteralExpressionSyntax literal) return Optional.of(literal.value());
        if (value instanceof IntegerLiteralExpressionSyntax literal) return Optional.of(Integer.toString(literal.value()));
        if (value instanceof DecimalLiteralExpressionSyntax literal) return Optional.of(literal.value().toPlainString());
        if (value instanceof BooleanLiteralExpressionSyntax literal) return Optional.of(Boolean.toString(literal.value()));
        return Optional.empty();
    }
}
