package io.github.kxyr0.echoeffects.echoscript;

import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.echoscript.binding.EffectResourceBinder;
import io.github.kxyr0.echoeffects.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EchoScriptEffectCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final EffectResourceBinder binder;

    public EchoScriptEffectCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new EffectResourceBinder());
    }

    public EchoScriptEffectCompiler(EchoScriptParser parser, EchoScriptDiagnosticAdapter diagnosticAdapter,
                                    EffectResourceBinder binder) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
    }

    public EffectCompileResult compile(String sourceId, String sourceText) {
        ParseResult parse = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<EffectDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parse.diagnostics()));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) {
            return new EffectCompileResult(Optional.empty(), diagnostics);
        }
        EffectCompileResult bound = binder.bind(parse.syntaxTree().orElseThrow());
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(EffectDiagnostic::isError)) {
            return new EffectCompileResult(Optional.empty(), diagnostics);
        }
        return new EffectCompileResult(bound.definition(), diagnostics);
    }
}
