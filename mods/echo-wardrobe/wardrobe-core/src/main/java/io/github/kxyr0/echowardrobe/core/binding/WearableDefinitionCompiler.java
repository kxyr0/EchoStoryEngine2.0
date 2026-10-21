package io.github.kxyr0.echowardrobe.core.binding;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.core.equipment.ItemRegistryView;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses `.wearable.es` source and delegates semantic binding.
 */
public final class WearableDefinitionCompiler {
    private final EchoScriptParser parser;
    private final WearableDefinitionBinder binder = new WearableDefinitionBinder();

    public WearableDefinitionCompiler(EchoScriptParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public WardrobeResult<List<WearableDefinition>> compile(
            String sourceId,
            String sourceText,
            String fingerprint,
            ItemRegistryView itemRegistry
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
        WardrobeResult<List<WearableDefinition>> result = binder.bind(parse.syntaxTree().orElseThrow(),
                sourceId, fingerprint, itemRegistry);
        diagnostics.addAll(result.diagnostics());
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(result.value().orElseThrow(), diagnostics);
    }
}
