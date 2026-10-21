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

public final class NpcSleepCommandReader {
    private final NpcStateArguments arguments = new NpcStateArguments();

    public Optional<CompiledDialogueCommand> sleepAt(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.sleep(call, actorResolver, fieldResolver);
        if (!CommandChecks.required(values, "npc", "bedX", "bedY", "bedZ")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-060",
                    "Npc.SleepAt requires npc, bedX, bedY and bedZ arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand("echo_npc:sleep_at", position, values));
    }

    public Optional<CompiledDialogueCommand> wakeUp(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.wake(call, actorResolver);
        if (!CommandChecks.required(values, "npc")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-061",
                    "Npc.WakeUp requires npc argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand("echo_npc:wake_up", position, values));
    }
}
