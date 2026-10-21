package io.github.kxyr0.echodialogue.echoscript.binding.highlevel;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entry and statically-known reachability validation for high-level dialogue graphs.
 */
final class HighLevelGraphAnalysis {
    Optional<DialogueNodeId> entry(List<DialogueNodeDeclarationSyntax> nodes, Map<String, DialogueNodeId> ids,
            List<DialogueDiagnostic> diagnostics) {
        if (nodes.size() == 1) {
            return Optional.ofNullable(ids.get(nodes.get(0).name()));
        }
        List<DialogueNodeDeclarationSyntax> starts = nodes.stream()
                .filter(DialogueNodeDeclarationSyntax::entry).toList();
        if (starts.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_DIALOGUE_ENTRY_MISSING",
                    "Dialogue with multiple nodes requires exactly one start Node."));
            return Optional.empty();
        }
        if (starts.size() > 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_DIALOGUE_ENTRY_AMBIGUOUS",
                    "Dialogue declares more than one start Node.", starts.get(1)));
            return Optional.empty();
        }
        return Optional.ofNullable(ids.get(starts.get(0).name()));
    }

    void warnUnreachable(DialogueNodeId entry, List<CompiledDialogueNode> nodes,
            Map<DialogueNodeId, DialogueNodeDeclarationSyntax> syntax, List<DialogueDiagnostic> diagnostics) {
        Set<DialogueNodeId> reached = new HashSet<>();
        ArrayDeque<DialogueNodeId> pending = new ArrayDeque<>();
        pending.add(entry);
        while (!pending.isEmpty()) {
            DialogueNodeId id = pending.removeFirst();
            if (!reached.add(id)) {
                continue;
            }
            nodes.stream().filter(node -> node.id().equals(id)).findFirst().ifPresent(node -> edges(node)
                    .stream().filter(edge -> !reached.contains(edge)).forEach(pending::addLast));
        }
        syntax.forEach((id, node) -> {
            if (!reached.contains(id)) {
                diagnostics.add(DialogueSemanticDiagnostics.warning("ECHO_SCRIPT_DIALOGUE_NODE_UNREACHABLE",
                        "Dialogue node is statically unreachable: " + id, node));
            }
        });
    }

    private Set<DialogueNodeId> edges(CompiledDialogueNode node) {
        Set<DialogueNodeId> values = new HashSet<>();
        node.transition().flatMap(CompiledDialogueNode.Transition::targetNodeId).ifPresent(values::add);
        node.choices().forEach(choice -> values.add(choice.targetNodeId()));
        return values;
    }
}
