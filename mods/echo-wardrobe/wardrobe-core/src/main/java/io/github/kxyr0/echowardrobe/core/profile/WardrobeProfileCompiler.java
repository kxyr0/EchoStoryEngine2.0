package io.github.kxyr0.echowardrobe.core.profile;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses `.wardrobe-profile.es` source and delegates semantic binding.
 */
public final class WardrobeProfileCompiler {
    private final EchoScriptParser parser;
    private final WardrobeProfileBinder binder = new WardrobeProfileBinder();

    public WardrobeProfileCompiler(EchoScriptParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public WardrobeResult<List<WardrobeProfileDefinition>> compile(
            String sourceId,
            String sourceText,
            String fingerprint
    ) {
        ParseResult parse = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        parse.diagnostics().forEach(diagnostic -> diagnostics.add(WardrobeDiagnostic.error(
                WardrobeDiagnosticCode.WARDROBE_DEFINITION_PARSE_ERROR,
                diagnostic.message(),
                diagnostic.sourceId().value())));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) {
            return WardrobeResult.invalid(diagnostics);
        }
        WardrobeResult<List<WardrobeProfileDefinition>> result = binder.bind(parse.syntaxTree().orElseThrow(),
                sourceId, fingerprint);
        diagnostics.addAll(result.diagnostics());
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(result.value().orElseThrow(), diagnostics);
    }
}
