package io.github.kxyr0.echotrigger.forge.authoring;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceValidationRequest;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.core.binding.TriggerDefinitionBinder;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echotrigger.core.reload.TriggerResourceFingerprint;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

final class TriggerAuthoringCompiler {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    TriggerAuthoringCompileResult compile(TriggerSourceValidationRequest request, TriggerRevision revision) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId(request.sourceName()), request.sourceText()));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            return new TriggerAuthoringCompileResult(Optional.empty(), parserDiagnostics(parsed),
                    fingerprint(request.sourceText()), 1, 0);
        }
        TriggerCompileResult bound = binder.bind(parsed.syntaxTree().orElseThrow(),
                fingerprint(request.sourceText()), revision);
        return new TriggerAuthoringCompileResult(bound.definition(), binderDiagnostics(bound.diagnostics()),
                fingerprint(request.sourceText()), 1, 1);
    }

    io.github.kxyr0.echotrigger.forge.resource.inline.InlineTriggerCompilation compileCatalog(
            TriggerSourceValidationRequest request, TriggerRevision revision) {
        var resource = new io.github.kxyr0.echotrigger.forge.resource.TriggerResourceText(
                java.nio.file.Path.of(request.sourceName()), request.sourceText());
        var inline = new io.github.kxyr0.echotrigger.forge.resource.inline.InlineTriggerCompiler()
                .compile(resource, revision);
        if (inline.handled()) {
            return inline;
        }
        var legacy = compile(request, revision);
        return new io.github.kxyr0.echotrigger.forge.resource.inline.InlineTriggerCompilation(
                true, legacy.definition().stream().toList(), legacy.diagnostics());
    }

    private static String fingerprint(String sourceText) {
        return TriggerResourceFingerprint.sha256(sourceText).value();
    }

    private static List<AreaDiagnosticView> parserDiagnostics(ParseResult result) {
        return result.diagnostics().stream().map(TriggerAuthoringCompiler::parserDiagnostic).toList();
    }

    private static AreaDiagnosticView parserDiagnostic(LanguageDiagnostic diagnostic) {
        OptionalInt line = diagnostic.span().map(span -> OptionalInt.of(span.start().line()))
                .orElseGet(OptionalInt::empty);
        OptionalInt column = diagnostic.span().map(span -> OptionalInt.of(span.start().column()))
                .orElseGet(OptionalInt::empty);
        return new AreaDiagnosticView(diagnostic.code(), diagnostic.message(), line, column);
    }

    private static List<AreaDiagnosticView> binderDiagnostics(List<TriggerDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> AreaDiagnosticView.withoutPosition(
                        diagnostic.code().name(), diagnostic.message()))
                .toList();
    }
}
