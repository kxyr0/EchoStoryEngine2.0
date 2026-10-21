package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.echoscript.DialogueCompileResult;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.binding.node.NodeContentReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Preserves compact ES1 and strict legacy DialogueResource authoring semantics.
 */
final class LegacyDialogueBinder {
    private final InitMethodReader initReader = new InitMethodReader();
    private final NodeContentReader nodeReader;

    LegacyDialogueBinder(List<DialogueLineArgumentBinder> extensions) {
        nodeReader = new NodeContentReader(List.copyOf(extensions));
    }

    DialogueCompileResult bind(CompilationUnitSyntax unit) {
        List<DialogueDiagnostic> diagnostics = new ArrayList<>();
        MethodCatalog methods = MethodCatalog.from(unit.classDeclaration().members());
        Map<String, DialogueNodeId> methodNodeIds = readMethodNodeIds(methods, diagnostics);
        Optional<DialogueId> id = readDialogueId(methods, diagnostics);
        Optional<DialogueNodeId> startNode = initReader.readStart(methods, methodNodeIds, diagnostics);
        AliasTable aliases = initReader.readAliases(methods, diagnostics);
        List<CompiledDialogueNode> nodes = readNodes(methods, methodNodeIds, aliases, diagnostics);
        validateGraph(startNode, nodes, diagnostics);
        if (diagnostics.stream().anyMatch(DialogueDiagnostic::isError) || id.isEmpty() || startNode.isEmpty()) {
            return new DialogueCompileResult(Optional.empty(), diagnostics);
        }
        return new DialogueCompileResult(Optional.of(new CompiledDialogue(
                id.orElseThrow(), startNode.orElseThrow(), nodes)), diagnostics);
    }

    private Optional<DialogueId> readDialogueId(MethodCatalog methods, List<DialogueDiagnostic> diagnostics) {
        Optional<DialogueId> initId = initReader.readDialogueId(methods, diagnostics);
        if (initId.isPresent()) {
            return initId;
        }
        Optional<MethodSyntax> build = methods.find("Build");
        if (build.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-002",
                    "DialogueResource does not contain Id(...) or Build()."));
            return Optional.empty();
        }
        diagnostics.add(DialogueSemanticDiagnostics.warning("DIALOGUE_LEGACY_AUTHORING_SYNTAX",
                "Legacy Build()/Dialogue(...) authoring is supported for compatibility.", build.orElseThrow()));
        return build.orElseThrow().body().statements().stream()
                .filter(ReturnStatementSyntax.class::isInstance).map(ReturnStatementSyntax.class::cast)
                .flatMap(statement -> statement.value().stream())
                .filter(InvocationExpressionSyntax.class::isInstance).map(InvocationExpressionSyntax.class::cast)
                .filter(call -> CallSyntax.name(call).equals("Dialogue")).findFirst()
                .flatMap(call -> readDialogueIdArgument(call, diagnostics));
    }

    private Optional<DialogueId> readDialogueIdArgument(InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics) {
        Optional<String> raw = CallSyntax.positionalString(call, 0);
        if (raw.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-003",
                    "Dialogue(...) must contain stable DialogueId.", call));
            return Optional.empty();
        }
        try {
            return Optional.of(DialogueId.parse(raw.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-013", exception.getMessage(), call));
            return Optional.empty();
        }
    }

    private Map<String, DialogueNodeId> readMethodNodeIds(MethodCatalog methods,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, DialogueNodeId> ids = new LinkedHashMap<>();
        for (MethodSyntax method : methods.nodeMethods()) {
            methods.nodeId(method, diagnostics).ifPresent(nodeId -> ids.put(method.name(), nodeId));
        }
        return ids;
    }

    private List<CompiledDialogueNode> readNodes(MethodCatalog methods, Map<String, DialogueNodeId> methodNodeIds,
            AliasTable aliases, List<DialogueDiagnostic> diagnostics) {
        Set<DialogueNodeId> seen = new LinkedHashSet<>();
        List<CompiledDialogueNode> nodes = new ArrayList<>();
        for (MethodSyntax method : methods.nodeMethods()) {
            Optional<DialogueNodeId> nodeId = Optional.ofNullable(methodNodeIds.get(method.name()));
            if (nodeId.isPresent() && !seen.add(nodeId.orElseThrow())) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-017",
                        "Node ID declared more than once: " + nodeId.orElseThrow(), method));
            }
            NodeContentReader.ReadContext context = new NodeContentReader.ReadContext(
                    aliases, methods, methodNodeIds, diagnostics, Optional.empty(), false);
            nodeId.flatMap(value -> nodeReader.readNode(value, method.body().statements(), context))
                    .ifPresent(nodes::add);
        }
        return List.copyOf(nodes);
    }

    private void validateGraph(Optional<DialogueNodeId> startNode, List<CompiledDialogueNode> nodes,
            List<DialogueDiagnostic> diagnostics) {
        Set<DialogueNodeId> nodeIds = nodes.stream().map(CompiledDialogueNode::id).collect(Collectors.toSet());
        if (startNode.isPresent() && !nodeIds.contains(startNode.orElseThrow())) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-007",
                    "Start node not found: " + startNode.orElseThrow()));
        }
        Set<ChoiceId> choiceIds = new LinkedHashSet<>();
        for (CompiledDialogueNode node : nodes) {
            validateNode(node, nodeIds, choiceIds, diagnostics);
        }
    }

    private void validateNode(CompiledDialogueNode node, Set<DialogueNodeId> nodeIds, Set<ChoiceId> choiceIds,
            List<DialogueDiagnostic> diagnostics) {
        for (CompiledDialogueChoice choice : node.choices()) {
            if (!choiceIds.add(choice.id())) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-026",
                        "ChoiceId declared more than once: " + choice.id()));
            }
            if (!nodeIds.contains(choice.targetNodeId())) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-027",
                        "Choice target node not found: " + choice.targetNodeId()));
            }
        }
        node.transition().flatMap(CompiledDialogueNode.Transition::targetNodeId)
                .filter(target -> !nodeIds.contains(target))
                .ifPresent(target -> diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-035",
                        "Dialogue.GoTo target node not found: " + target)));
    }
}
