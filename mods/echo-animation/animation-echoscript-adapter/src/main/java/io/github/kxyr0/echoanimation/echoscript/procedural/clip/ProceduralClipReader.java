package io.github.kxyr0.echoanimation.echoscript.procedural.clip;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.procedural.ProceduralSyntax;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.List;
import java.util.Optional;

/**
 * Binds procedural AnimationClipResource Build statements.
 */
public final class ProceduralClipReader {
    private final ClipKeyReader keys = new ClipKeyReader();

    public AnimationCompileResult read(MethodSyntax build, List<AnimationDiagnostic> diagnostics) {
        ClipDslDraft draft = new ClipDslDraft();
        try {
            for (StatementSyntax statement : build.body().statements()) {
                readStatement(draft, statement);
            }
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.of(draft.build()), Optional.empty(), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-CLIP-BUILDER",
                    exception.getMessage(), build.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private void readStatement(ClipDslDraft draft, StatementSyntax statement) {
        if (statement instanceof LocalDeclarationStatementSyntax local) {
            InvocationExpressionSyntax call = local.initializer().map(ProceduralSyntax::invocation)
                    .orElseThrow(() -> new IllegalArgumentException("Local '" + local.name()
                            + "' requires an intrinsic initializer."));
            switch (CallSyntax.name(call)) {
                case "Segment" -> draft.segment(local.name(), call);
                case "Track" -> draft.track(local.name(), call);
                case "Point" -> draft.point(call);
                default -> throw new IllegalArgumentException("Unsupported clip local intrinsic: "
                        + CallSyntax.name(call));
            }
            return;
        }
        if (statement instanceof ExpressionStatementSyntax expression) {
            readCall(draft, ProceduralSyntax.invocation(expression.expression()));
            return;
        }
        throw new IllegalArgumentException("Procedural clip Build supports only local declarations and calls.");
    }

    private void readCall(ClipDslDraft draft, InvocationExpressionSyntax call) {
        String base = baseName(call);
        switch (base) {
            case "Id" -> draft.id(call);
            case "Rig" -> draft.rig(call);
            case "Duration" -> draft.duration(call);
            case "Loop" -> draft.loop(call);
            case "Speed" -> draft.speed(call);
            case "Point" -> draft.point(call);
            case "Event" -> draft.event(call);
            case "RootMotion" -> draft.rootMotion(call);
            case "Key" -> draft.key(keys.read(call, draft.tracks()));
            default -> throw new IllegalArgumentException("Unsupported clip builder intrinsic: " + base);
        }
    }

    private String baseName(InvocationExpressionSyntax call) {
        InvocationExpressionSyntax current = call;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            current = parent;
        }
        return CallSyntax.name(current);
    }
}
