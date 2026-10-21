package io.github.kxyr0.echodialogue.echoscript.binding.command.core;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import java.util.ArrayList;
import java.util.List;

public final class CompiledCommandValidator {
    public List<CompiledDialogueCommand> valid(
            List<CompiledDialogueCommand> commands,
            int lineCount,
            List<DialogueDiagnostic> diagnostics) {
        List<CompiledDialogueCommand> output = new ArrayList<>();
        for (CompiledDialogueCommand command : commands) {
            if (lineCount == 0 || command.position() > lineCount) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic(
                        "EDLG-SEM-045",
                        "External command must be attached to a Line position in the same node."));
            } else {
                output.add(command);
            }
        }
        return List.copyOf(output);
    }
}
