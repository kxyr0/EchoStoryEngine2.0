package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.identity.SpellDefinitionRevision;
import io.github.kxyr0.echomagic.echoscript.binding.SpellDefinitionBinder;
import io.github.kxyr0.echomagic.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class EchoScriptSpellCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnostics;
    private final SpellDefinitionBinder binder;

    public EchoScriptSpellCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new SpellDefinitionBinder());
    }

    public EchoScriptSpellCompiler(EchoScriptParser parser, EchoScriptDiagnosticAdapter diagnostics,
                                   SpellDefinitionBinder binder) {
        this.parser = parser;
        this.diagnostics = diagnostics;
        this.binder = binder;
    }

    public SpellCompileResult compile(String sourceId, String sourceText) {
        ParseResult parse = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        List<MagicDiagnostic> all = new ArrayList<>(diagnostics.adapt(parse.diagnostics()));
        if (!parse.successful() || parse.syntaxTree().isEmpty()) return new SpellCompileResult(Optional.empty(), all);
        SpellCompileResult bound = binder.bind(parse.syntaxTree().orElseThrow(), revision(sourceText));
        all.addAll(bound.diagnostics());
        return all.stream().anyMatch(MagicDiagnostic::isError)
                ? new SpellCompileResult(Optional.empty(), all) : new SpellCompileResult(bound.definition(), all);
    }

    private SpellDefinitionRevision revision(String source) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return new SpellDefinitionRevision(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java.", exception);
        }
    }
}
