package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.definition.ItemDefinitionResult;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * EchoScript syntax parser followed by item-specific weapon semantic binding.
 */
public final class EchoScriptWeaponCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final WeaponDefinitionBinder binder;

    public EchoScriptWeaponCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new WeaponDefinitionBinder());
    }

    public EchoScriptWeaponCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            WeaponDefinitionBinder binder
    ) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
    }

    public ItemDefinitionResult<WeaponDefinition> compile(
            String sourceId,
            String sourceText,
            ItemDefinitionRevision revision
    ) {
        ParseResult parse = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<ItemDefinitionDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parse.diagnostics()));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) {
            return ItemDefinitionResult.invalid(diagnostics);
        }
        ItemSourceFingerprint fingerprint = ItemSourceFingerprint.ofBytes(sourceText.getBytes(StandardCharsets.UTF_8));
        ItemDefinitionResult<WeaponDefinition> bound = binder.bind(
                parse.syntaxTree().orElseThrow(), sourceId, fingerprint, revision);
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            return ItemDefinitionResult.invalid(diagnostics);
        }
        return ItemDefinitionResult.valid(bound.definition().orElseThrow(), diagnostics);
    }
}
