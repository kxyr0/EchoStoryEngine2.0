package io.github.kxyr0.echodialogue.echoscript.binding.command.npc;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NpcMovementCommandReader {
    private final NpcMovementArguments arguments = new NpcMovementArguments();

    public Optional<CompiledDialogueCommand> move(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics,
            boolean sprint) {
        String gait = sprint ? "SPRINT" : "WALK";
        String name = sprint ? "Npc.Sprint" : "Npc.Move";
        Map<String, String> values = new LinkedHashMap<>(arguments.movement(call, actorResolver));
        values.put("gait", gait);
        if (!CommandChecks.required(values, "npc", "x", "y", "z")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-042",
                    name + " requires npc and Vector3 or npc, x, y, z arguments.",
                    call));
            return Optional.empty();
        }
        if (values.containsKey("speed")
                && !CommandChecks.positiveDouble(values.get("speed"))) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-042",
                    name + " speed must be positive.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_navigation:npc_move",
                position,
                Map.copyOf(values)));
    }

    public Optional<CompiledDialogueCommand> stop(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.movement(call, actorResolver);
        if (!CommandChecks.required(values, "npc")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-043",
                    "Npc.Stop requires npc argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_navigation:npc_stop",
                position,
                values));
    }

    public Optional<CompiledDialogueCommand> arrival(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.arrival(call, actorResolver);
        if (!CommandChecks.required(values, "npc")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-067",
                    "LineAfterMove requires speaker alias or npc argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_navigation:npc_move_arrival",
                position,
                values));
    }
}
