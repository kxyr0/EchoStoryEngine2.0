package io.github.kxyr0.echothoughtspace.forge.client.style;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationKey;
import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ThoughtCombinationBinder {
    List<ThoughtCombinationRecipe> bind(ClassSyntax resource, String namespace, String resourcePath) {
        List<ThoughtCombinationRecipe> result = new ArrayList<>();
        for (MethodSyntax method : resource.members().stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).toList()) {
            if (!method.returnType().name().text().equals("Combination")) continue;
            result.add(recipe(method, namespace, resourcePath));
        }
        return List.copyOf(result);
    }

    private static ThoughtCombinationRecipe recipe(MethodSyntax method, String namespace, String resourcePath) {
        if (!method.parameters().isEmpty()) throw new IllegalArgumentException("Combination takes no parameters.");
        Map<String, InvocationExpressionSyntax> calls = calls(method);
        InvocationExpressionSyntax from = required(calls, "From");
        InvocationExpressionSyntax thought = required(calls, "Thought");
        List<io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax> inputs =
                ThoughtScriptCallReader.positional(from);
        if (inputs.size() != 2) throw new IllegalArgumentException("From(...) requires exactly two thought IDs.");
        List<io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax> labels =
                ThoughtScriptCallReader.positional(thought);
        if (labels.size() != 1) throw new IllegalArgumentException("Thought(...) requires exactly one text.");
        ThoughtNodeId left = new ThoughtNodeId(ThoughtScriptCallReader.string(inputs.get(0), "From[0]"));
        ThoughtNodeId right = new ThoughtNodeId(ThoughtScriptCallReader.string(inputs.get(1), "From[1]"));
        ThoughtNodeId result = resultId(calls.get("Id"), method.name(), namespace, resourcePath);
        return new ThoughtCombinationRecipe(new ThoughtCombinationKey(left, right), result,
                new ThoughtNodeLabel(ThoughtScriptCallReader.string(labels.get(0), "Thought")));
    }

    private static Map<String, InvocationExpressionSyntax> calls(MethodSyntax method) {
        Map<String, InvocationExpressionSyntax> result = new LinkedHashMap<>();
        for (var statement : method.body().statements()) {
            if (!(statement instanceof ExpressionStatementSyntax expression)
                    || !(expression.expression() instanceof InvocationExpressionSyntax invocation)) {
                throw new IllegalArgumentException("Combination body accepts From, Thought and optional Id only.");
            }
            String name = ThoughtScriptCallReader.name(invocation);
            if (!List.of("From", "Thought", "Id").contains(name)) {
                throw new IllegalArgumentException("Unknown Combination call: " + name);
            }
            if (result.put(name, invocation) != null) {
                throw new IllegalArgumentException(name + " is declared more than once in " + method.name() + '.');
            }
        }
        return result;
    }

    private static InvocationExpressionSyntax required(Map<String, InvocationExpressionSyntax> calls, String name) {
        InvocationExpressionSyntax value = calls.get(name);
        if (value == null) throw new IllegalArgumentException("Combination is missing " + name + "(...).");
        return value;
    }

    static ThoughtNodeId resultId(InvocationExpressionSyntax idOverride, String symbol,
            String namespace, String resourcePath) {
        if (idOverride == null) return new ThoughtNodeId(namespace + ':' + resourcePath + '#' + normalize(symbol));
        var values = ThoughtScriptCallReader.positional(idOverride);
        if (values.size() != 1) throw new IllegalArgumentException("Id(...) requires exactly one ID.");
        return new ThoughtNodeId(ThoughtScriptCallReader.string(values.get(0), "Id"));
    }

    private static String normalize(String value) {
        String normalized = value.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Za-z])([0-9])", "$1_$2")
                .replaceAll("([0-9])([A-Za-z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("Combination name cannot produce an empty ID.");
        return normalized;
    }
}
