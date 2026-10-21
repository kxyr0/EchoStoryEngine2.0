package io.github.kxyr0.echodialogue.echoscript.binding.highlevel;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.echoscript.DialogueCompileResult;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.binding.AliasTable;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;
import io.github.kxyr0.echodialogue.echoscript.binding.SpeakerDeclarationReader;
import io.github.kxyr0.echodialogue.echoscript.binding.node.NodeContentReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echodialogue.echoscript.resource.SourceResourceContext;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Analyzes and lowers typed high-level DialogueResource syntax to the strict compiled graph.
 */
public final class HighLevelDialogueBinder {
    private final NodeContentReader nodeReader;
    private final SpeakerDeclarationReader speakerReader = new SpeakerDeclarationReader();
    private final FormerNodeIdReader formerReader = new FormerNodeIdReader();
    private final HighLevelGraphAnalysis graph = new HighLevelGraphAnalysis();
    private final HighLevelNodeAnalysis nodeAnalysis = new HighLevelNodeAnalysis();

    public HighLevelDialogueBinder(List<DialogueLineArgumentBinder> extensions) {
        nodeReader = new NodeContentReader(List.copyOf(extensions));
    }

    public DialogueCompileResult bind(CompilationUnitSyntax unit, ClassSyntax resource,
            Optional<SourceResourceContext> context) {
        List<DialogueDiagnostic> diagnostics = new ArrayList<>();
        List<DialogueNodeDeclarationSyntax> declarations = resource.members().stream()
                .filter(DialogueNodeDeclarationSyntax.class::isInstance)
                .map(DialogueNodeDeclarationSyntax.class::cast).toList();
        if (declarations.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_DIALOGUE_NODE_MISSING",
                    "DialogueResource must declare at least one Node.", resource));
        }
        validateMode(resource, diagnostics);
        Optional<SourceResourceContext> checkedContext = requireContext(context, resource, diagnostics);
        Map<String, DialogueNodeId> nodeIds = nodeAnalysis.ids(declarations, diagnostics);
        Optional<DialogueNodeId> entry = graph.entry(declarations, nodeIds, diagnostics);
        Optional<DialogueId> dialogueId = dialogueId(resource, checkedContext, diagnostics);
        AliasTable aliases = speakers(resource, checkedContext, diagnostics);
        MethodCatalog methods = MethodCatalog.from(resource.members());
        NodeContentReader.ReadContext readContext = readContext(nodeIds, aliases, methods, checkedContext,
                diagnostics);
        List<CompiledDialogueNode> nodes = readNodes(declarations, nodeIds, readContext);
        Map<DialogueNodeId, DialogueNodeId> formerIds = formerReader.read(
                classCalls(resource, "Formerly"), nodeIds, diagnostics);
        validateTargets(nodes, nodeIds, diagnostics);
        if (entry.isPresent()) {
            graph.warnUnreachable(entry.orElseThrow(), nodes, nodeAnalysis.syntaxById(declarations, nodeIds),
                    diagnostics);
        }
        if (diagnostics.stream().anyMatch(DialogueDiagnostic::isError) || dialogueId.isEmpty() || entry.isEmpty()) {
            return new DialogueCompileResult(Optional.empty(), diagnostics);
        }
        CompiledDialogue.SourceInfo sourceInfo = sourceInfo(unit, checkedContext.orElseThrow(), resource, nodeIds);
        CompiledDialogue dialogue = new CompiledDialogue(dialogueId.orElseThrow(), entry.orElseThrow(), nodes,
                formerIds, Optional.of(sourceInfo));
        return new DialogueCompileResult(Optional.of(dialogue), diagnostics);
    }

    private void validateMode(ClassSyntax resource, List<DialogueDiagnostic> diagnostics) {
        boolean conflict = resource.members().stream().anyMatch(member -> member instanceof MethodSyntax method
                && (Set.of("Build", "Init", "Nodes").contains(method.name())
                        || method.returnType().name().text().equals("NodeDef")))
                || resource.members().stream().anyMatch(member -> member instanceof FieldSyntax field
                        && field.type().name().text().equals("NodeDef"));
        if (conflict) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_DIALOGUE_DEFINITION_CONFLICT",
                    "High-level Node declarations cannot be combined with a legacy dialogue graph.", resource));
        }
    }

    private Optional<SourceResourceContext> requireContext(Optional<SourceResourceContext> context,
            ClassSyntax resource, List<DialogueDiagnostic> diagnostics) {
        if (context.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_RESOURCE_CONTEXT_MISSING",
                    "High-level DialogueResource requires content ownership and a source file name.", resource));
        }
        return context;
    }

    private Optional<DialogueId> dialogueId(ClassSyntax resource, Optional<SourceResourceContext> context,
            List<DialogueDiagnostic> diagnostics) {
        List<InvocationExpressionSyntax> calls = classCalls(resource, "Id");
        if (calls.size() > 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_RESOURCE_ID_OVERRIDE_CONFLICT",
                    "DialogueResource declares more than one Id override.", calls.get(1)));
            return Optional.empty();
        }
        if (calls.isEmpty()) {
            return context.map(SourceResourceContext::inferredDialogueId);
        }
        Optional<String> raw = CallSyntax.positionalString(calls.get(0), 0);
        if (raw.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_RESOURCE_ID_OVERRIDE_CONFLICT",
                    "Id requires a namespaced resource ID.", calls.get(0)));
            return Optional.empty();
        }
        try {
            return Optional.of(DialogueId.parse(raw.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_RESOURCE_ID_OVERRIDE_CONFLICT",
                    exception.getMessage(), calls.get(0)));
            return Optional.empty();
        }
    }

    private AliasTable speakers(ClassSyntax resource, Optional<SourceResourceContext> context,
            List<DialogueDiagnostic> diagnostics) {
        AliasTable aliases = new AliasTable();
        if (context.isPresent()) {
            classCalls(resource, "Speaker").forEach(call -> speakerReader.read(call,
                    context.orElseThrow().resourceNamespace(), aliases, diagnostics));
        }
        return aliases;
    }

    private NodeContentReader.ReadContext readContext(Map<String, DialogueNodeId> nodeIds, AliasTable aliases,
            MethodCatalog methods, Optional<SourceResourceContext> context,
            List<DialogueDiagnostic> diagnostics) {
        Optional<String> namespace = context.map(SourceResourceContext::resourceNamespace);
        return new NodeContentReader.ReadContext(
                aliases, methods, nodeIds, diagnostics, namespace, true);
    }

    private List<CompiledDialogueNode> readNodes(List<DialogueNodeDeclarationSyntax> declarations,
            Map<String, DialogueNodeId> nodeIds, NodeContentReader.ReadContext readContext) {
        List<CompiledDialogueNode> nodes = new ArrayList<>();
        declarations.forEach(node -> nodeReader.readNode(nodeIds.get(node.name()), node.body().statements(),
                readContext).ifPresent(nodes::add));
        return List.copyOf(nodes);
    }

    private void validateTargets(List<CompiledDialogueNode> nodes, Map<String, DialogueNodeId> nodeIds,
            List<DialogueDiagnostic> diagnostics) {
        Set<DialogueNodeId> ids = new LinkedHashSet<>(nodeIds.values());
        nodes.forEach(node -> {
            node.choices().stream().filter(choice -> !ids.contains(choice.targetNodeId())).forEach(choice ->
                    diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_NOT_FOUND",
                            "Choice target node not found: " + choice.targetNodeId())));
            node.transition().flatMap(CompiledDialogueNode.Transition::targetNodeId)
                    .filter(target -> !ids.contains(target)).ifPresent(target -> diagnostics.add(
                            DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_NODE_NOT_FOUND",
                                    "GoTo target node not found: " + target)));
        });
    }

    private List<InvocationExpressionSyntax> classCalls(ClassSyntax resource, String name) {
        return resource.members().stream().filter(ClassCallSyntax.class::isInstance).map(ClassCallSyntax.class::cast)
                .map(ClassCallSyntax::invocation).filter(call -> CallSyntax.name(call).equals(name)).toList();
    }

    private CompiledDialogue.SourceInfo sourceInfo(CompilationUnitSyntax unit, SourceResourceContext context,
            ClassSyntax resource, Map<String, DialogueNodeId> ids) {
        Map<String, DialogueNodeDeclarationSyntax> syntax = new LinkedHashMap<>();
        resource.members().stream().filter(DialogueNodeDeclarationSyntax.class::isInstance)
                .map(DialogueNodeDeclarationSyntax.class::cast).forEach(node -> syntax.put(node.name(), node));
        Map<DialogueNodeId, CompiledDialogue.NodeSource> sources = new LinkedHashMap<>();
        ids.forEach((symbol, id) -> sources.put(id, new CompiledDialogue.NodeSource(symbol,
                syntax.get(symbol).span().start().line(), syntax.get(symbol).span().start().column())));
        Optional<String> namespace = unit.namespaceDeclaration().map(value -> value.name().text());
        return new CompiledDialogue.SourceInfo(context.sourceId(), namespace,
                classCalls(resource, "Id").isEmpty(), true, sources);
    }
}
