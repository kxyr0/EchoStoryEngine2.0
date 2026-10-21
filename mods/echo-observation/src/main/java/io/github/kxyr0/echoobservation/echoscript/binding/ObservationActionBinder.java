package io.github.kxyr0.echoobservation.echoscript.binding;

import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptArgument;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptBlock;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic.Severity;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import java.util.ArrayList;
import java.util.List;

final class ObservationActionBinder {
    ObservationScriptBlock bind(SectionStatementSyntax section, List<ObservationDiagnostic> diagnostics) {
        List<ObservationScriptAction> actions = new ArrayList<>();
        section.body().statements().forEach(statement -> {
            if (!(statement instanceof ExpressionStatementSyntax expression)
                    || !(expression.expression() instanceof InvocationExpressionSyntax call)) {
                error(diagnostics, statement.span().startOffset(), "OnComplete accepts linear script calls only.");
                return;
            }
            List<ObservationScriptArgument> arguments = new ArrayList<>();
            if (new io.github.kxyr0.echoaudio.script.AudioCommandBinder().accepts(call)) {
                try {
                    actions.add(new io.github.kxyr0.echoobservation.echoscript.audio.ObservationAudioBinder().bind(call));
                } catch (io.github.kxyr0.echoaudio.api.AudioFailure failure) {
                    error(diagnostics, call.span().startOffset(), failure.getMessage());
                }
                return;
            }
            for (var argument : call.arguments()) {
                var scalar = ObservationCallReader.scalar(argument.value());
                if (scalar.isEmpty()) {
                    error(diagnostics, argument.span().startOffset(), "Script action argument must be scalar.");
                    return;
                }
                arguments.add(new ObservationScriptArgument(argument.name(), scalar.orElseThrow()));
            }
            String target = ObservationCallReader.name(call);
            if (target.isBlank()) {
                error(diagnostics, call.span().startOffset(), "Script action target is missing.");
                return;
            }
            actions.add(new ObservationScriptAction(target, arguments, call.span().startOffset()));
        });
        return new ObservationScriptBlock(actions);
    }

    private void error(List<ObservationDiagnostic> diagnostics, int offset, String message) {
        diagnostics.add(new ObservationDiagnostic(ObservationDiagnosticCode.OBS_ACTION_INVALID,
                Severity.ERROR, message, offset));
    }
}
