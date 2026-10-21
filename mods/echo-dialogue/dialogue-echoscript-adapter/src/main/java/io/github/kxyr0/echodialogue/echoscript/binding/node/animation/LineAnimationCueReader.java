package io.github.kxyr0.echodialogue.echoscript.binding.node.animation;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import java.util.List;
import java.util.Optional;

/** Reads the established animation methods chained after a Line call. */
public final class LineAnimationCueReader {
    private static final String BODY = "bodyAnimation";
    private static final String FACE = "faceAnimation";
    private static final String FACE_CLEAR = "faceAnimationClear";
    private static final String FACE_STOP = "faceAnimationStop";

    public void read(List<CallSyntax.InvocationSegment> chain, MethodCatalog methods,
            List<CompiledDialogueLine.Cue> cues, List<DialogueDiagnostic> diagnostics) {
        for (int index = 1; index < chain.size(); index++) {
            CallSyntax.InvocationSegment segment = chain.get(index);
            switch (segment.name()) {
                case "SetAnimation" -> addAnimation(segment.invocation(), methods, cues, diagnostics, false);
                case "SetAnimationFace" -> addAnimation(segment.invocation(), methods, cues, diagnostics, true);
                case "SetAnimationFaceClear" -> addTerminal(segment.invocation(), cues, diagnostics, FACE_CLEAR);
                case "StopFaceAnimation" -> addTerminal(segment.invocation(), cues, diagnostics, FACE_STOP);
                default -> diagnostics.add(DialogueSemanticDiagnostics.unsupported(
                        "Line chain '" + segment.name() + "'", segment.invocation()));
            }
        }
    }

    private void addAnimation(InvocationExpressionSyntax call, MethodCatalog methods,
            List<CompiledDialogueLine.Cue> cues, List<DialogueDiagnostic> diagnostics, boolean face) {
        if (conflict(cues, face)) {
            duplicate(call, diagnostics, face);
            return;
        }
        if (call.arguments().size() != 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-036",
                    "Animation setter requires exactly one argument.", call));
            return;
        }
        ExpressionSyntax value = call.arguments().get(0).value();
        validateType(value, methods, diagnostics, call, face);
        animationValue(value, methods).ifPresent(id ->
                cues.add(new CompiledDialogueLine.Cue(face ? FACE : BODY, id)));
    }

    private void addTerminal(InvocationExpressionSyntax call, List<CompiledDialogueLine.Cue> cues,
            List<DialogueDiagnostic> diagnostics, String cueName) {
        if (conflict(cues, true)) {
            duplicate(call, diagnostics, true);
            return;
        }
        if (!call.arguments().isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-036",
                    cueName + " requires no arguments.", call));
            return;
        }
        cues.add(new CompiledDialogueLine.Cue(cueName, "true"));
    }

    private boolean conflict(List<CompiledDialogueLine.Cue> cues, boolean face) {
        if (!face) {
            return cues.stream().anyMatch(cue -> cue.name().equals(BODY));
        }
        return cues.stream().anyMatch(cue -> cue.name().equals(FACE)
                || cue.name().equals(FACE_CLEAR) || cue.name().equals(FACE_STOP));
    }

    private void validateType(ExpressionSyntax expression, MethodCatalog methods,
            List<DialogueDiagnostic> diagnostics, InvocationExpressionSyntax call, boolean face) {
        if (expression instanceof NameExpressionSyntax name) {
            String expected = face ? "FaceAnimationId" : "BodyAnimationId";
            methods.fieldType(name.name()).filter(type -> !type.equals(expected)).ifPresent(type ->
                    diagnostics.add(DialogueSemanticDiagnostics.semantic("DIALOGUE_WRONG_ANIMATION_ID_TYPE",
                            "Animation setter expects " + expected + " but got " + type + ".", call)));
        }
    }

    private Optional<String> animationValue(ExpressionSyntax expression, MethodCatalog methods) {
        if (expression instanceof NameExpressionSyntax name) {
            return methods.fieldStringValue(name.name()).or(() -> Optional.of(name.name()));
        }
        return CallSyntax.simpleValue(expression);
    }

    private void duplicate(InvocationExpressionSyntax call, List<DialogueDiagnostic> diagnostics, boolean face) {
        String code = face ? "DIALOGUE_DUPLICATE_FACE_ANIMATION" : "DIALOGUE_DUPLICATE_BODY_ANIMATION";
        diagnostics.add(DialogueSemanticDiagnostics.semantic(code, "Duplicate animation setter.", call));
    }
}
