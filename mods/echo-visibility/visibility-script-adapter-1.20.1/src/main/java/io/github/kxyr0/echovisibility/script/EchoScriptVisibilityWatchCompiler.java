package io.github.kxyr0.echovisibility.script;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.script.binding.VisibilityWatchBinder;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import java.util.Optional;

public final class EchoScriptVisibilityWatchCompiler {
    private final VisibilityWatchBinder binder = new VisibilityWatchBinder();

    public VisibilityWatchCompileResult compile(
            String sourceId,
            String source,
            VisibilityDefinitionRevision revision) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(
                ParseRequest.ofV1(new SourceId(sourceId), source));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            List<VisibilityDiagnostic> diagnostics = parsed.diagnostics().stream().map(diagnostic ->
                    new VisibilityDiagnostic(VisibilityDiagnosticCode.VISIBILITY_WATCH_INVALID,
                            diagnostic.code() + ": " + diagnostic.message(), Optional.empty(),
                            Optional.empty(), Optional.of(diagnostic.sourceId().value()))).toList();
            return new VisibilityWatchCompileResult(Optional.empty(), diagnostics, 1, 0);
        }
        return binder.bind(parsed.syntaxTree().orElseThrow(), sourceId, source, revision);
    }
}
