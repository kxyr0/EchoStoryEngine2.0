package io.github.kxyr0.echodialogue.echoscript.binding.highlevel;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves high-level source node symbols to canonical runtime node IDs.
 */
final class HighLevelNodeAnalysis {
    Map<String, DialogueNodeId> ids(List<DialogueNodeDeclarationSyntax> declarations,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, DialogueNodeId> values = new LinkedHashMap<>();
        Map<DialogueNodeId, DialogueNodeDeclarationSyntax> seen = new LinkedHashMap<>();
        for (DialogueNodeDeclarationSyntax node : declarations) {
            DialogueNodeId id = explicitId(node, diagnostics)
                    .orElseGet(() -> new DialogueNodeId(NodeNameNormalizer.normalize(node.name())));
            DialogueNodeDeclarationSyntax previous = seen.putIfAbsent(id, node);
            if (previous != null) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_ID_COLLISION",
                        "Node symbols normalize to the same NodeId " + id + "; first declaration is at line "
                                + previous.span().start().line() + ".", node));
            }
            values.put(node.name(), id);
        }
        return Map.copyOf(values);
    }

    Map<DialogueNodeId, DialogueNodeDeclarationSyntax> syntaxById(
            List<DialogueNodeDeclarationSyntax> declarations, Map<String, DialogueNodeId> ids) {
        Map<DialogueNodeId, DialogueNodeDeclarationSyntax> values = new LinkedHashMap<>();
        declarations.forEach(node -> values.put(ids.get(node.name()), node));
        return values;
    }

    private Optional<DialogueNodeId> explicitId(DialogueNodeDeclarationSyntax node,
            List<DialogueDiagnostic> diagnostics) {
        List<InvocationExpressionSyntax> calls = node.body().statements().stream()
                .flatMap(statement -> CallSyntax.statementCall(statement).stream())
                .filter(call -> CallSyntax.name(call).equals("NodeId")).toList();
        if (calls.isEmpty()) {
            return Optional.empty();
        }
        if (calls.size() > 1 || CallSyntax.positionalString(calls.get(0), 0).isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_ID_COLLISION",
                    "NodeId requires one unique string override.", calls.get(calls.size() - 1)));
            return Optional.empty();
        }
        try {
            return Optional.of(new DialogueNodeId(CallSyntax.positionalString(calls.get(0), 0).orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_ID_COLLISION",
                    exception.getMessage(), calls.get(0)));
            return Optional.empty();
        }
    }
}
