package io.github.kxyr0.echonpc.echoscript;

import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.binding.form.FormResourceBinder;
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
 * EchoScript syntax parser facade followed by NpcFormResource semantic binding.
 */
public final class EchoScriptNpcFormCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final FormResourceBinder binder;

    public EchoScriptNpcFormCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new FormResourceBinder());
    }

    public EchoScriptNpcFormCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            FormResourceBinder binder
    ) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
    }

    public NpcFormCompileResult compile(String sourceId, String sourceText, NpcFormRevision revision) {
        ParseResult parsed = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<NpcDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parsed.diagnostics()));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            return new NpcFormCompileResult(Optional.empty(), diagnostics);
        }
        NpcFormCompileResult bound = binder.bind(sourceId, parsed.syntaxTree().orElseThrow(), revision);
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            return new NpcFormCompileResult(Optional.empty(), diagnostics);
        }
        return new NpcFormCompileResult(bound.form(), diagnostics);
    }
}
