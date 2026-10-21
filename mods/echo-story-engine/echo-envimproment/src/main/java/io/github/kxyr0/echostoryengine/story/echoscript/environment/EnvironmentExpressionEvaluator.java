package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ParenthesizedExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.intrinsic.EnvironmentIntrinsicFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class EnvironmentExpressionEvaluator {
    private final EnvironmentSourceDiagnostics.SourceContext source;
    private final EnvironmentSourceDiagnostics diagnostics;
    private final Map<String, MethodSyntax> methods;
    private final List<StoryDiagnostic> values;
    private final ArrayDeque<String> callStack = new ArrayDeque<>();
    private final EnvironmentIntrinsicFactory intrinsics;

    EnvironmentExpressionEvaluator(
            EnvironmentSourceDiagnostics.SourceContext source,
            EnvironmentSourceDiagnostics diagnostics,
            Map<String, MethodSyntax> methods,
            List<StoryDiagnostic> values) {
        this.source = source;
        this.diagnostics = diagnostics;
        this.methods = Map.copyOf(methods);
        this.values = values;
        this.intrinsics = new EnvironmentIntrinsicFactory(source, diagnostics, values);
    }

    EnvironmentValue evaluate(ExpressionSyntax expression, Map<String, EnvironmentValue> symbols) {
        if (expression instanceof StringLiteralExpressionSyntax text) {
            return new EnvironmentValue.Text(text.value());
        }
        if (expression instanceof IntegerLiteralExpressionSyntax number) {
            return new EnvironmentValue.Number(number.value());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return symbol(name.name(), name.span(), symbols);
        }
        if (expression instanceof ArrayLiteralExpressionSyntax array) {
            return array(array, symbols);
        }
        if (expression instanceof ParenthesizedExpressionSyntax parenthesized) {
            return evaluate(parenthesized.expression(), symbols);
        }
        if (expression instanceof InvocationExpressionSyntax invocation) {
            return invocation(invocation, symbols);
        }
        return unsupportedExpression(expression.span());
    }

    EnvironmentValue evaluateMethod(MethodSyntax method, Map<String, EnvironmentValue> fields) {
        if (callStack.contains(method.name())) {
            return recursive(method);
        }
        callStack.push(method.name());
        Map<String, EnvironmentValue> locals = new LinkedHashMap<>(fields);
        for (StatementSyntax statement : method.body().statements()) {
            Optional<EnvironmentValue> returned = statement(statement, method.name(), locals);
            if (returned.isPresent()) {
                callStack.pop();
                return returned.orElseThrow();
            }
        }
        callStack.pop();
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-019",
                "Environment method must return a value.", source, method.span(), Map.of("method", method.name()))));
        return invalid();
    }

    private Optional<EnvironmentValue> statement(
            StatementSyntax statement,
            String methodName,
            Map<String, EnvironmentValue> locals) {
        if (statement instanceof LocalDeclarationStatementSyntax local) {
            EnvironmentValue value = local.initializer()
                    .map(initializer -> evaluate(initializer, locals))
                    .orElseGet(EnvironmentValue.Invalid::new);
            locals.put(local.name(), value);
            return Optional.empty();
        }
        if (statement instanceof ReturnStatementSyntax returned && returned.value().isPresent()) {
            return Optional.of(evaluate(returned.value().orElseThrow(), locals));
        }
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-020",
                "Environment method contains unsupported statement.", source, statement.span(),
                Map.of("method", methodName))));
        return Optional.of(invalid());
    }

    private EnvironmentValue invocation(
            InvocationExpressionSyntax invocation,
            Map<String, EnvironmentValue> symbols) {
        String target = targetName(invocation.target());
        if (intrinsics.isIntrinsic(target)) {
            return intrinsics.create(target, arguments(invocation, symbols), invocation.span());
        }
        MethodSyntax method = methods.get(target);
        if (method != null) {
            return evaluateMethod(method, symbols);
        }
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-021",
                "Environment source references unknown method or intrinsic '" + target + "'.",
                source, invocation.span(), Map.of("target", target))));
        return invalid();
    }

    private List<EnvironmentIntrinsicFactory.ArgumentValue> arguments(
            InvocationExpressionSyntax invocation,
            Map<String, EnvironmentValue> symbols) {
        List<EnvironmentIntrinsicFactory.ArgumentValue> arguments = new ArrayList<>();
        invocation.arguments().forEach(argument -> arguments.add(new EnvironmentIntrinsicFactory.ArgumentValue(
                argument.name(), evaluate(argument.value(), symbols), argument.span())));
        return List.copyOf(arguments);
    }

    private String targetName(ExpressionSyntax expression) {
        if (expression instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (expression instanceof MemberAccessExpressionSyntax member) {
            return targetName(member.target()) + "." + member.memberName();
        }
        return "";
    }

    private EnvironmentValue array(
            ArrayLiteralExpressionSyntax array,
            Map<String, EnvironmentValue> symbols) {
        List<EnvironmentValue> items = new ArrayList<>();
        array.elements().forEach(element -> items.add(evaluate(element, symbols)));
        return new EnvironmentValue.Array(items);
    }

    private EnvironmentValue symbol(String name, SourceSpan span, Map<String, EnvironmentValue> symbols) {
        EnvironmentValue value = symbols.get(name);
        if (value != null) {
            return value;
        }
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-022",
                "Environment source references unknown value '" + name + "'.", source, span,
                Map.of("name", name))));
        return invalid();
    }

    private EnvironmentValue unsupportedExpression(SourceSpan span) {
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-023",
                "Environment source contains unsupported expression.", source, span, Map.of())));
        return invalid();
    }

    private EnvironmentValue recursive(MethodSyntax method) {
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-024",
                "Environment method recursion is not supported.", source, method.span(),
                Map.of("method", method.name()))));
        return invalid();
    }

    private EnvironmentValue invalid() {
        return new EnvironmentValue.Invalid();
    }
}
