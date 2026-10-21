package io.github.kxyr0.echoscene.compile.pipeline;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.compile.SceneCommandRegistry;
import io.github.kxyr0.echoscene.compile.SceneCompileResult;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.binding.BindingResult;
import io.github.kxyr0.echoscene.compile.binding.SceneResourceBinder;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import java.util.List;
import java.util.Optional;

public final class SceneCompiler {
    private final EchoScriptParser parser;
    private final SceneResourceBinder binder;

    public SceneCompiler(SceneSymbolCatalog symbols) {
        this(EchoScriptLanguage.parser(), symbols, SceneCommandRegistry.standard());
    }

    public SceneCompiler(EchoScriptParser parser, SceneSymbolCatalog symbols, SceneCommandRegistry commands) {
        this.parser = parser;
        binder = new SceneResourceBinder(symbols, commands);
    }

    public SceneCompileResult compile(SceneSourceInput input) {
        ParseResult parsed = parser.parse(ParseRequest.ofV1(new SourceId(input.sourcePath()), input.sourceText()));
        List<SceneDiagnostic> parserDiagnostics = parsed.diagnostics().stream().map(value -> new SceneDiagnostic(
                value.severity() == DiagnosticSeverity.ERROR ? SceneDiagnostic.Severity.ERROR
                        : SceneDiagnostic.Severity.WARNING,
                value.code(), value.message(), value.span().orElseGet(() -> origin(value.sourceId())))).toList();
        if (!parsed.successful()) {
            return new SceneCompileResult(Optional.empty(), Optional.empty(), parserDiagnostics);
        }
        BindingResult bound = binder.bind(parsed.syntaxTree().orElseThrow(), input);
        List<SceneDiagnostic> diagnostics = java.util.stream.Stream.concat(parserDiagnostics.stream(),
                bound.diagnostics().stream()).toList();
        return new SceneCompileResult(bound.definition(), bound.descriptor(), diagnostics);
    }

    private SourceSpan origin(SourceId sourceId) {
        SourcePosition position = new SourcePosition(0, 1, 1);
        return new SourceSpan(sourceId, 0, 0, position, position);
    }
}
