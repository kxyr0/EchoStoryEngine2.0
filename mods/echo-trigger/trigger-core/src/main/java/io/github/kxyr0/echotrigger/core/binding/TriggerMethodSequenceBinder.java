package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.definition.TriggerSequenceStep;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.IfStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.ArrayList;
import java.util.List;

final class TriggerMethodSequenceBinder {
    private final TriggerExpressionBinder expressions = new TriggerExpressionBinder();

    List<TriggerSequenceStep> bind(
            MethodSyntax method,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics
    ) {
        List<TriggerSequenceStep> steps = new ArrayList<>();
        for (StatementSyntax statement : method.body().statements()) {
            readStatement(statement, fields, steps, diagnostics);
        }
        if (steps.isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.INVALID_SEQUENCE,
                    "Trigger sequence must contain at least one Wait(...)."));
        }
        return List.copyOf(steps);
    }

    private void readStatement(
            StatementSyntax statement,
            TriggerFieldCatalog fields,
            List<TriggerSequenceStep> steps,
            List<TriggerDiagnostic> diagnostics
    ) {
        if (statement instanceof IfStatementSyntax branch) {
            validateIf(branch, diagnostics);
            return;
        }
        TriggerCallSyntax.statementCall(statement).ifPresent(call -> readCall(call, fields, steps, diagnostics));
    }

    private void readCall(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerSequenceStep> steps,
            List<TriggerDiagnostic> diagnostics
    ) {
        if (TriggerCallSyntax.name(call).equals("Wait")) {
            readWait(call, fields, steps, diagnostics);
        } else if (expressions.isEdgeEvent(call)) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.UNSUPPORTED_TRIGGER_SYNTAX,
                    "Edge trigger events are only valid inside Wait(...)."));
        }
    }

    private void readWait(
            InvocationExpressionSyntax wait,
            TriggerFieldCatalog fields,
            List<TriggerSequenceStep> steps,
            List<TriggerDiagnostic> diagnostics
    ) {
        TriggerCallSyntax.positionalInvocation(wait, 0)
                .flatMap(call -> expressions.bind(call, fields, diagnostics))
                .map(expression -> new TriggerSequenceStep(steps.size(), expression))
                .ifPresent(steps::add);
    }

    private void validateIf(IfStatementSyntax branch, List<TriggerDiagnostic> diagnostics) {
        if (branch.condition() instanceof InvocationExpressionSyntax call && expressions.isEdgeEvent(call)) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.UNSUPPORTED_TRIGGER_SYNTAX,
                    "if is synchronous; use Wait(...) for PlayerEnter/PlayerExit/PlayerStay."));
        }
    }
}
