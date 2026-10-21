package io.github.kxyr0.echodialogue.echoscript.binding.highlevel;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads explicit, deterministic class-level old-node migration aliases.
 */
final class FormerNodeIdReader {
    Map<DialogueNodeId, DialogueNodeId> read(List<InvocationExpressionSyntax> calls,
            Map<String, DialogueNodeId> nodeIds, List<DialogueDiagnostic> diagnostics) {
        Map<DialogueNodeId, DialogueNodeId> aliases = new LinkedHashMap<>();
        Set<DialogueNodeId> canonical = Set.copyOf(nodeIds.values());
        for (InvocationExpressionSyntax call : calls) {
            readCall(call, nodeIds, canonical, aliases, diagnostics);
        }
        return Map.copyOf(aliases);
    }

    private void readCall(InvocationExpressionSyntax call, Map<String, DialogueNodeId> nodeIds,
            Set<DialogueNodeId> canonical, Map<DialogueNodeId, DialogueNodeId> aliases,
            List<DialogueDiagnostic> diagnostics) {
        var former = CallSyntax.positionalString(call, 0);
        var symbol = CallSyntax.positionalName(call, 1);
        if (call.arguments().size() != 2 || former.isEmpty() || symbol.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_FORMER_ID_COLLISION",
                    "Formerly requires an old NodeId string and a current Node symbol.", call));
            return;
        }
        DialogueNodeId current = nodeIds.get(symbol.orElseThrow());
        if (current == null) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_NOT_FOUND",
                    "Formerly target node not found: " + symbol.orElseThrow(), call));
            return;
        }
        add(former.orElseThrow(), new MappingContext(current, canonical, aliases, diagnostics, call));
    }

    private void add(String raw, MappingContext context) {
        try {
            DialogueNodeId former = new DialogueNodeId(raw);
            if (context.canonical().contains(former)
                    || context.aliases().putIfAbsent(former, context.current()) != null) {
                context.diagnostics().add(DialogueSemanticDiagnostics.semantic(
                        "ECHO_SCRIPT_NODE_FORMER_ID_COLLISION",
                        "Former NodeId maps to more than one current node: " + former, context.call()));
            }
        } catch (IllegalArgumentException exception) {
            context.diagnostics().add(DialogueSemanticDiagnostics.semantic(
                    "ECHO_SCRIPT_NODE_FORMER_ID_COLLISION", exception.getMessage(), context.call()));
        }
    }

    private record MappingContext(DialogueNodeId current, Set<DialogueNodeId> canonical,
            Map<DialogueNodeId, DialogueNodeId> aliases, List<DialogueDiagnostic> diagnostics,
            InvocationExpressionSyntax call) {
    }
}
