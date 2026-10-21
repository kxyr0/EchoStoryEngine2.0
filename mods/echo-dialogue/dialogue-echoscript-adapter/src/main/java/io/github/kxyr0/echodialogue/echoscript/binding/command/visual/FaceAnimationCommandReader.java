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

public final class FaceAnimationCommandReader {
    private final VisualArguments arguments = new VisualArguments();

    public Optional<CompiledDialogueCommand> set(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> subjectResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.face(call, subjectResolver, fieldResolver);
        if (!CommandChecks.required(values, "subject", "animation")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-053",
                    "SetFaceAnimation requires npcId and animationId arguments.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                "echo_animation:set_face_animation",
                position,
                values));
    }

    public Optional<CompiledDialogueCommand> clear(
            InvocationExpressionSyntax call,
            int position,
            Function<String, String> subjectResolver,
            boolean all,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.clearFace(call, subjectResolver, all);
        if (all && !values.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-065",
                    "ClearFaceAnimations requires no arguments.",
                    call));
            return Optional.empty();
        }
        if (!all && !CommandChecks.required(values, "subject")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-066",
                    "ClearFaceAnimation requires npcId argument.",
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(
                all
                        ? "echo_animation:clear_all_face_animations"
                        : "echo_animation:clear_face_animation",
                position,
                values));
    }
}
