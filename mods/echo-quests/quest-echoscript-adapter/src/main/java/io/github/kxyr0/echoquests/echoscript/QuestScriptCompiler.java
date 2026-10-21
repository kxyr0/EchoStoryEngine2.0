package io.github.kxyr0.echoquests.echoscript;

import io.github.kxyr0.echoquests.api.diagnostic.QuestDiagnostic;
import io.github.kxyr0.echoquests.api.diagnostic.QuestDiagnostic.Severity;
import io.github.kxyr0.echoquests.api.diagnostic.QuestDiagnosticCode;
import io.github.kxyr0.echoquests.api.model.QuestDefinition;
import io.github.kxyr0.echoquests.api.source.QuestSourceRef;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import io.github.kxyr0.echoquests.echoscript.binding.QuestResourceBinder;
import io.github.kxyr0.echoquests.echoscript.binding.QuestSourceContext;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuestScriptCompiler {
    private final EchoScriptParser parser;
    private final QuestResourceBinder binder = new QuestResourceBinder();

    public QuestScriptCompiler() { this(EchoScriptLanguage.parser()); }
    public QuestScriptCompiler(EchoScriptParser parser) { this.parser = parser; }

    public QuestCompileResult compile(List<QuestScriptSource> sources) {
        List<QuestDiagnostic> diagnostics = new ArrayList<>();
        List<QuestDefinition> definitions = new ArrayList<>();
        for (QuestScriptSource source : sources) compile(source, definitions, diagnostics);
        if (diagnostics.stream().anyMatch(QuestDiagnostic::error)) return new QuestCompileResult(Optional.empty(), diagnostics);
        try {
            return new QuestCompileResult(Optional.of(QuestCatalog.of(definitions)), diagnostics);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(new QuestDiagnostic(QuestDiagnosticCode.DEFINITION_INVALID, Severity.ERROR,
                    message(exception), Optional.empty()));
            return new QuestCompileResult(Optional.empty(), diagnostics);
        }
    }

    private void compile(QuestScriptSource source, List<QuestDefinition> definitions,
            List<QuestDiagnostic> diagnostics) {
        var result = parser.parse(ParseRequest.ofV1(new SourceId(source.sourceId()), source.text()));
        result.diagnostics().forEach(value -> diagnostics.add(new QuestDiagnostic(QuestDiagnosticCode.SCRIPT_INVALID,
                value.severity() == DiagnosticSeverity.ERROR ? Severity.ERROR : Severity.WARNING,
                value.code() + ": " + value.message(), value.span().map(span -> new QuestSourceRef(
                        source.sourceId(), span.start().line(), span.start().column())))));
        Optional<QuestSourceContext> context = QuestSourceContext.resolve(source.sourceId());
        if (context.isEmpty()) {
            diagnostics.add(new QuestDiagnostic(QuestDiagnosticCode.SCRIPT_INVALID, Severity.ERROR,
                    "Quest ID cannot be inferred from source path.", Optional.empty()));
            return;
        }
        if (!result.successful() || result.syntaxTree().isEmpty()) return;
        try {
            definitions.add(binder.bind(result.syntaxTree().orElseThrow().classDeclaration(), context.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            var span = result.syntaxTree().orElseThrow().classDeclaration().span();
            diagnostics.add(new QuestDiagnostic(QuestDiagnosticCode.DEFINITION_INVALID, Severity.ERROR,
                    message(exception), Optional.of(new QuestSourceRef(source.sourceId(), span.start().line(),
                            span.start().column()))));
        }
    }

    private String message(IllegalArgumentException exception) {
        return exception.getMessage() == null ? "Invalid quest definition." : exception.getMessage();
    }
}
