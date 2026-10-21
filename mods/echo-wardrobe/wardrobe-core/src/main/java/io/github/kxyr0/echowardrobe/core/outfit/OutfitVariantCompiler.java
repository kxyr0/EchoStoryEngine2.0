package io.github.kxyr0.echowardrobe.core.outfit;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.core.asset.SkinAssetView;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses `.outfit.es` source and delegates semantic binding.
 */
public final class OutfitVariantCompiler {
    private final EchoScriptParser parser;
    private final OutfitVariantBinder binder = new OutfitVariantBinder();

    public OutfitVariantCompiler(EchoScriptParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public WardrobeResult<List<OutfitVariantDefinition>> compile(
            String sourceId,
            String sourceText,
            String fingerprint,
            SkinAssetView assets
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
        WardrobeResult<List<OutfitVariantDefinition>> result = binder.bind(parse.syntaxTree().orElseThrow(),
                sourceId, fingerprint, assets);
        diagnostics.addAll(result.diagnostics());
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(result.value().orElseThrow(), diagnostics);
    }
}
