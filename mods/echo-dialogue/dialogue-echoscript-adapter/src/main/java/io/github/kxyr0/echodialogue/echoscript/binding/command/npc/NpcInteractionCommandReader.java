package io.github.kxyr0.echodialogue.echoscript.binding.command.npc;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NpcInteractionCommandReader {
    private final NpcInteractionArguments arguments = new NpcInteractionArguments();

    public Optional<CompiledDialogueCommand> interact(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            Function<String, Optional<Map<String, String>>> blockPositionResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.interaction(
                call,
                actorResolver,
                blockPositionResolver);
        if (!CommandChecks.required(values, "npc", "x", "y", "z")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-068",
                    "Npc.Interact requires npc and BlockPosition or npc, x, y, z arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_npc_interaction:interact",
                position,
                values));
    }

    public Optional<CompiledDialogueCommand> stop(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return npcOnly(
                call,
                position,
                actorResolver,
                diagnostics,
                new NpcOnlySpec("EDLG-SEM-069", "Npc.StopInteraction requires npc argument.",
                        "echo_npc_interaction:stop"));
    }

    public Optional<CompiledDialogueCommand> standUp(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return npcOnly(
                call,
                position,
                actorResolver,
                diagnostics,
                new NpcOnlySpec("EDLG-SEM-070", "Npc.StandUp requires npc argument.",
                        "echo_npc_interaction:stand_up"));
    }

    private Optional<CompiledDialogueCommand> npcOnly(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics,
            NpcOnlySpec spec) {
        Map<String, String> values = arguments.npcOnly(call, actorResolver);
        if (!CommandChecks.required(values, "npc")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    spec.diagnosticCode(),
                    spec.diagnosticMessage(),
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(spec.commandType(), position, values));
    }

    private record NpcOnlySpec(String diagnosticCode, String diagnosticMessage, String commandType) {
    }
}
