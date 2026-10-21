package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EffectCommandReader {
    private final ExternalArguments arguments = new ExternalArguments();

    public Optional<CompiledDialogueCommand> applyToPlayer(
            InvocationExpressionSyntax call,
            int position,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.effect(call);
        if (!CommandChecks.required(values, "effectId")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-070",
                    "Effect.ApplyToPlayer requires effectId argument.",
                    call));
            return Optional.empty();
        }
        if (!CommandChecks.positiveDouble(values.get("intensity"))) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-071",
                    "Effect.ApplyToPlayer requires positive intensity.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_effects:apply_to_player",
                position,
                values));
    }
}
