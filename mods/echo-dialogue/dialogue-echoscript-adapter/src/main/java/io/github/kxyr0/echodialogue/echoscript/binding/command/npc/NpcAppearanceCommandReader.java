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

public final class NpcAppearanceCommandReader {
    private final NpcStateArguments arguments = new NpcStateArguments();

    public Optional<CompiledDialogueCommand> setForm(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.form(call, actorResolver, fieldResolver);
        if (!CommandChecks.required(values, "npc", "form")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-059",
                    "Npc.SetForm requires npc and form arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand("echo_npc:set_form", position, values));
    }

    public Optional<CompiledDialogueCommand> changeSkin(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.skin(call, actorResolver, fieldResolver);
        if (!CommandChecks.required(values, "npc", "skin")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-075",
                    "Npc.ChangeSkin requires npc and skin arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand("echo_npc:change_skin", position, values));
    }
}
