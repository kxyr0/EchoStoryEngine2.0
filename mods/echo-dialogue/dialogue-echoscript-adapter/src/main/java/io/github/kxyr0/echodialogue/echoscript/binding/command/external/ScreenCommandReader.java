package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ScreenCommandReader {
    private final ExternalArguments arguments = new ExternalArguments();

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            int position,
            String waitType,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.screen(call);
        if (!CommandChecks.required(values, "canvasGroup", "time")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-050",
                    "Screen.SetBlack requires canvasGroup and time arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(waitType, position, values));
    }
}
