package io.github.kxyr0.echodialogue.echoscript.binding.command.visual;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class BlinkTimingCommandReader {
    private final VisualArguments arguments = new VisualArguments();

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> subjectResolver,
            boolean clear,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.blink(call, subjectResolver);
        if (!CommandChecks.required(values, "subject")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-055",
                    "Blink timing command requires npcId argument.",
                    call));
            return Optional.empty();
        }
        if (!clear && !CommandChecks.required(
                values,
                "periodTicks", "closeTicks", "holdTicks", "openTicks")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-056",
                    "SetBlinkTiming requires periodTicks, closeTicks, holdTicks and openTicks arguments.",
                    call));
            return Optional.empty();
        }
        if (!clear && !CommandChecks.validBlinkTiming(values)) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-057",
                    "SetBlinkTiming requires positive period/close/open ticks, non-negative holdTicks, "
                            + "and periodTicks greater than closeTicks + holdTicks + openTicks.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                clear
                        ? "echo_animation:clear_blink_timing"
                        : "echo_animation:set_blink_timing",
                position,
                values));
    }
}
