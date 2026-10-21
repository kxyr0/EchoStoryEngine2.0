package io.github.kxyr0.echoplayer.core.template;

import io.github.kxyr0.echoplayer.api.result.PlayerDiagnosticSeverity;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;

/**
 * EchoScript parser plus player-template binder.
 */
public final class EchoScriptPlayerTemplateCompiler {
    private final EchoScriptParser parser;
    private final PlayerTemplateBinder binder;
    private final TemplateFingerprint fingerprint;

    public EchoScriptPlayerTemplateCompiler() {
        this(EchoScriptLanguage.parser(), new PlayerTemplateBinder(), new TemplateFingerprint());
    }

    public EchoScriptPlayerTemplateCompiler(EchoScriptParser parser, PlayerTemplateBinder binder,
                                            TemplateFingerprint fingerprint) {
        this.parser = parser;
        this.binder = binder;
        this.fingerprint = fingerprint;
    }

    public PlayerProfileResult<PlayerProfileTemplate> compile(String sourceId, String sourceText) {
        var parse = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<PlayerProfileDiagnostic> diagnostics = new ArrayList<>(adapt(parse.diagnostics()));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) {
            return PlayerProfileResult.invalid(diagnostics);
        }
        PlayerProfileResult<PlayerProfileTemplate> bound = binder.bind(parse.syntaxTree().orElseThrow(),
                sourceId, fingerprint.ofText(sourceText));
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(PlayerProfileDiagnostic::error)) {
            return PlayerProfileResult.invalid(diagnostics);
        }
        return PlayerProfileResult.valid(bound.value().orElseThrow(), diagnostics);
    }

    private List<PlayerProfileDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adapt).toList();
    }

    private PlayerProfileDiagnostic adapt(LanguageDiagnostic diagnostic) {
        PlayerDiagnosticSeverity severity = diagnostic.severity() == DiagnosticSeverity.ERROR
                ? PlayerDiagnosticSeverity.ERROR
                : PlayerDiagnosticSeverity.WARNING;
        return new PlayerProfileDiagnostic("PLAYER_TEMPLATE_PARSE_ERROR", severity,
                diagnostic.code() + ": " + diagnostic.message(), java.util.Optional.of(diagnostic.sourceId().value()));
    }
}
