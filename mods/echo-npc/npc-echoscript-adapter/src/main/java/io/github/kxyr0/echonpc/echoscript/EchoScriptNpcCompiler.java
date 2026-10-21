package io.github.kxyr0.echonpc.echoscript;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.binding.NpcResourceBinder;
import io.github.kxyr0.echonpc.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * EchoScript syntax parser facade followed by NpcResource semantic binding.
 */
public final class EchoScriptNpcCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final NpcResourceBinder binder;

    public EchoScriptNpcCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new NpcResourceBinder());
    }

    public EchoScriptNpcCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            NpcResourceBinder binder
    ) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
    }

    public NpcCompileResult compile(String sourceId, String sourceText) {
        ParseResult parseResult = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<NpcDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parseResult.diagnostics()));
        if (!parseResult.successful() || parseResult.syntaxTree().isEmpty()) {
            return new NpcCompileResult(Optional.empty(), diagnostics);
        }
        NpcCompileResult bound = binder.bind(parseResult.syntaxTree().orElseThrow(), sourceId);
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            return new NpcCompileResult(Optional.empty(), diagnostics);
        }
        return new NpcCompileResult(bound.definition(), diagnostics);
    }
}
