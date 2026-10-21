package io.github.kxyr0.echothoughtspace.forge.client.style;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ThoughtDeclarationBinder {
    List<ThoughtNodeSnapshot> bind(ClassSyntax resource, String namespace, String resourcePath) {
        List<ThoughtNodeSnapshot> result = new ArrayList<>();
        for (MethodSyntax method : resource.members().stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).toList()) {
            if (method.returnType().name().text().equals("Thought")) {
                result.add(thought(method, namespace, resourcePath));
            }
        }
        return List.copyOf(result);
    }

    private static ThoughtNodeSnapshot thought(MethodSyntax method, String namespace, String resourcePath) {
        if (!method.parameters().isEmpty()) throw new IllegalArgumentException("Thought takes no parameters.");
        Map<String, InvocationExpressionSyntax> calls = calls(method);
        InvocationExpressionSyntax text = calls.get("Text");
        if (text == null) throw new IllegalArgumentException("Thought is missing Text(...).");
        var values = ThoughtScriptCallReader.positional(text);
        if (values.size() != 1) throw new IllegalArgumentException("Text(...) requires exactly one string.");
        return new ThoughtNodeSnapshot(ThoughtCombinationBinder.resultId(calls.get("Id"), method.name(),
                namespace, resourcePath), new ThoughtNodeLabel(ThoughtScriptCallReader.string(values.get(0), "Text")),
                new ThoughtNodeRevision(0L));
    }

    private static Map<String, InvocationExpressionSyntax> calls(MethodSyntax method) {
        Map<String, InvocationExpressionSyntax> result = new LinkedHashMap<>();
        for (var statement : method.body().statements()) {
            if (!(statement instanceof ExpressionStatementSyntax expression)
                    || !(expression.expression() instanceof InvocationExpressionSyntax invocation)) {
                throw new IllegalArgumentException("Thought body accepts Text and optional Id only.");
            }
            String name = ThoughtScriptCallReader.name(invocation);
            if (!List.of("Text", "Id").contains(name)) {
                throw new IllegalArgumentException("Unknown Thought call: " + name);
            }
            if (result.put(name, invocation) != null) {
                throw new IllegalArgumentException(name + " is declared more than once in " + method.name() + '.');
            }
        }
        return result;
    }
}
