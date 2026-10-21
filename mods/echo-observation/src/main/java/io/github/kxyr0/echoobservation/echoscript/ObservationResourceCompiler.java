package io.github.kxyr0.echoobservation.echoscript;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic.Severity;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.echoscript.binding.ObservationResourceBinder;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ObservationResourceCompiler {
    private final ObservationResourceBinder binder = new ObservationResourceBinder();

    public ObservationCompileResult compile(String sourceId, String source) {
        ParseResult parse = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(sourceId), source));
        List<ObservationDiagnostic> diagnostics = new ArrayList<>();
        parse.diagnostics().forEach(value -> diagnostics.add(new ObservationDiagnostic(
                ObservationDiagnosticCode.OBS_RESOURCE_INVALID, Severity.ERROR,
                value.code() + ": " + value.message(), value.span().map(span -> span.startOffset()).orElse(0))));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) {
            return new ObservationCompileResult(Optional.empty(), diagnostics);
        }
        ObservationCompileResult bound = binder.bind(parse.syntaxTree().orElseThrow(), sourceId, source);
        diagnostics.addAll(bound.diagnostics());
        return new ObservationCompileResult(bound.resource(), diagnostics);
    }

    public boolean productionValid(ObservationCompileResult result) {
        return result.successful() && result.resource().orElseThrow().observations().stream()
                .allMatch(observation -> observation.placed() || observation.repeatable());
    }
}
