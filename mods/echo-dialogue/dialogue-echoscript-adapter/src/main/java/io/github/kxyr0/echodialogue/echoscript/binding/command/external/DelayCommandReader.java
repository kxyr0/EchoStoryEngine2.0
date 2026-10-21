package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DelayCommandReader {
    private final ExternalArguments arguments = new ExternalArguments();

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            int position,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.delay(call);
        if (call.arguments().size() != 1
                || CommandChecks.positiveInt(values.get("ticks")).isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-074",
                    "Delay requires exactly one positive integer ticks argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand("echo_dialogue:delay", position, values));
    }
}
