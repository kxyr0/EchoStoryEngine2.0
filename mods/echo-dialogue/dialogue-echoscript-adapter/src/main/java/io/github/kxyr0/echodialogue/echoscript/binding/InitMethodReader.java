package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InitMethodReader {
    Optional<DialogueId> readDialogueId(MethodCatalog methods, List<DialogueDiagnostic> diagnostics) {
        List<InvocationExpressionSyntax> calls = initCalls(methods, diagnostics).stream()
                .filter(call -> CallSyntax.name(call).equals("Id"))
                .toList();
        if (calls.isEmpty()) {
            return Optional.empty();
        }
        if (calls.size() > 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-033",
                    "Init() must contain at most one Id(...).", calls.get(1)));
            return Optional.empty();
        }
        return readDialogueId(calls.get(0), diagnostics);
    }

    Optional<DialogueNodeId> readStart(MethodCatalog methods, Map<String, DialogueNodeId> methodNodeIds,
            List<DialogueDiagnostic> diagnostics) {
        List<InvocationExpressionSyntax> calls = initCalls(methods, diagnostics).stream()
                .filter(call -> CallSyntax.name(call).equals("Start"))
                .toList();
        if (calls.size() != 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-004",
                    "Init() must contain exactly one Start(...)."));
            return Optional.empty();
        }
        return readStartId(calls.get(0), methodNodeIds, diagnostics);
    }

    AliasTable readAliases(MethodCatalog methods, List<DialogueDiagnostic> diagnostics) {
        AliasTable aliases = new AliasTable();
        initCalls(methods, diagnostics).stream()
                .filter(call -> CallSyntax.name(call).equals("Alias"))
                .forEach(call -> aliases.readAlias(call, diagnostics));
        return aliases;
    }

    private Optional<DialogueNodeId> readStartId(InvocationExpressionSyntax call,
            Map<String, DialogueNodeId> methodNodeIds, List<DialogueDiagnostic> diagnostics) {
        Optional<String> legacy = CallSyntax.positionalString(call, 0);
        if (legacy.isPresent()) {
            diagnostics.add(DialogueSemanticDiagnostics.warning(
                    "DIALOGUE_LEGACY_AUTHORING_SYNTAX",
                    "Legacy Start(\"node_id\") authoring is supported for compatibility.", call));
            return safeNodeId(legacy.orElseThrow(), call, diagnostics);
        }
        Optional<String> method = CallSyntax.positionalName(call, 0);
        if (method.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-014",
                    "Start requires a node method reference or legacy node ID.", call));
            return Optional.empty();
        }
        Optional<DialogueNodeId> nodeId = Optional.ofNullable(methodNodeIds.get(method.orElseThrow()));
        if (nodeId.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-034",
                    "Start target method is not a dialogue node: " + method.orElseThrow(), call));
        }
        return nodeId;
    }

    private Optional<DialogueId> readDialogueId(InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics) {
        Optional<String> raw = CallSyntax.positionalString(call, 0);
        if (raw.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-003",
                    "Id(...) requires stable DialogueId.", call));
            return Optional.empty();
        }
        try {
            return Optional.of(DialogueId.parse(raw.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-013", exception.getMessage(), call));
            return Optional.empty();
        }
    }

    private Optional<DialogueNodeId> safeNodeId(String raw, InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics) {
        try {
            return Optional.of(new DialogueNodeId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-015", exception.getMessage(), call));
            return Optional.empty();
        }
    }

    private List<InvocationExpressionSyntax> initCalls(MethodCatalog methods, List<DialogueDiagnostic> diagnostics) {
        Optional<MethodSyntax> init = methods.find("Init");
        if (init.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-012",
                    "DialogueResource does not contain Init()."));
            return List.of();
        }
        return init.orElseThrow().body().statements().stream()
                .flatMap(statement -> CallSyntax.statementCall(statement).stream())
                .toList();
    }
}
