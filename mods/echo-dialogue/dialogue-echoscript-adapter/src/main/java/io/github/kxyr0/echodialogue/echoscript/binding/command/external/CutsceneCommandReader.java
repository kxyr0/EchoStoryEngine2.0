package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class CutsceneCommandReader {
    private final ExternalArguments arguments = new ExternalArguments();

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            int position,
            String action,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.cutscene(call, fieldResolver);
        if (!CommandChecks.required(values, "cutscene")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-046",
                    "Cutscene command requires cutscene, id or first positional argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_cutscene:" + action,
                position,
                values));
    }
}
