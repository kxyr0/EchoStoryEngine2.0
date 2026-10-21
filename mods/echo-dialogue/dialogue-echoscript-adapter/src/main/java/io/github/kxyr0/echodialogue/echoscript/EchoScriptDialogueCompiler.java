package io.github.kxyr0.echodialogue.echoscript;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.echoscript.binding.DialogueResourceBinder;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echodialogue.echoscript.resource.SourceResourceContext;
import io.github.kxyr0.echodialogue.echoscript.resource.SourceResourceContextResolver;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Syntax-only EchoScript parser facade followed by DialogueResource semantic binding.
 */
public final class EchoScriptDialogueCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final DialogueResourceBinder binder;
    private final SourceResourceContextResolver contextResolver = new SourceResourceContextResolver();

    public EchoScriptDialogueCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new DialogueResourceBinder());
    }

    public EchoScriptDialogueCompiler(List<DialogueLineArgumentBinder> extensions) {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new DialogueResourceBinder(extensions));
    }

    public EchoScriptDialogueCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            DialogueResourceBinder binder
    ) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
    }

    public DialogueCompileResult compile(String sourceId, String sourceText) {
        return compile(contextResolver.resolveDialogue(sourceId), sourceId, sourceText);
    }

    public DialogueCompileResult compile(SourceResourceContext context, String sourceText) {
        return compile(Optional.of(context), context.sourceId(), sourceText);
    }

    private DialogueCompileResult compile(Optional<SourceResourceContext> sourceContext, String sourceId,
            String sourceText) {
        ParseResult parseResult = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<DialogueDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parseResult.diagnostics()));
        if (!parseResult.successful() || parseResult.syntaxTree().isEmpty()) {
            return new DialogueCompileResult(Optional.empty(), diagnostics);
        }
        DialogueCompileResult bound = binder.bind(parseResult.syntaxTree().orElseThrow(), sourceContext);
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(DialogueDiagnostic::isError)) {
            return new DialogueCompileResult(Optional.empty(), diagnostics);
        }
        return new DialogueCompileResult(bound.dialogue(), diagnostics);
    }
}
