package io.github.kxyr0.echobackgroundnpc.echoscript;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundAppearancePool;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcProfile;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic.Severity;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnosticCode;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalogValidator;
import io.github.kxyr0.echobackgroundnpc.echoscript.binding.AmbientDialogueResourceBinder;
import io.github.kxyr0.echobackgroundnpc.echoscript.binding.AppearancePoolResourceBinder;
import io.github.kxyr0.echobackgroundnpc.echoscript.binding.BackgroundResourceContext;
import io.github.kxyr0.echobackgroundnpc.echoscript.binding.NpcResourceBinder;
import io.github.kxyr0.echobackgroundnpc.echoscript.binding.ProfileResourceBinder;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parses all candidates, resolves typed NPC symbols, then atomically produces one catalog. */
public final class BackgroundScriptCompiler {
    private final EchoScriptParser parser;
    private final NpcResourceBinder npcs = new NpcResourceBinder();
    private final ProfileResourceBinder profiles = new ProfileResourceBinder();
    private final AppearancePoolResourceBinder pools = new AppearancePoolResourceBinder();
    private final AmbientDialogueResourceBinder dialogues = new AmbientDialogueResourceBinder();

    public BackgroundScriptCompiler() { this(EchoScriptLanguage.parser()); }
    public BackgroundScriptCompiler(EchoScriptParser parser) { this.parser = parser; }

    public BackgroundCompileResult compile(List<BackgroundScriptSource> sources, long revision) {
        List<BackgroundDiagnostic> diagnostics = new ArrayList<>();
        List<ParsedBackgroundScript> parsed = parse(sources, diagnostics);
        Map<String, BackgroundNpcId> symbols = symbols(parsed, diagnostics);
        List<BackgroundNpcDefinition> definitions = new ArrayList<>();
        List<BackgroundNpcProfile> profileValues = new ArrayList<>();
        List<BackgroundAppearancePool> poolValues = new ArrayList<>();
        List<AmbientDialogue> dialogueValues = new ArrayList<>();
        for (ParsedBackgroundScript script : parsed) {
            String base = script.unit().classDeclaration().baseType().name().text();
            try {
                switch (base) {
                    case "BackgroundNpcResource" -> definitions.add(npcs.bind(script.unit().classDeclaration(),
                            script.context(), revision, symbols));
                    case "BackgroundNpcProfileResource" -> profileValues.add(profiles.bind(
                            script.unit().classDeclaration(), script.context(), revision));
                    case "BackgroundAppearancePoolResource" -> poolValues.add(pools.bind(
                            script.unit().classDeclaration(), script.context(), revision));
                    case "AmbientDialogueResource" -> dialogueValues.add(dialogues.bind(
                            script.unit().classDeclaration(), script.context(), revision));
                    default -> diagnostics.add(error("Unknown background resource base: " + base, script));
                }
            } catch (IllegalArgumentException exception) {
                diagnostics.add(error(exception.getMessage(), script));
            }
        }
        if (diagnostics.stream().anyMatch(BackgroundDiagnostic::error)) return failed(diagnostics);
        BackgroundCatalog catalog;
        try {
            catalog = BackgroundCatalog.of(revision, definitions, profileValues, poolValues, dialogueValues);
        } catch (IllegalStateException exception) {
            diagnostics.add(new BackgroundDiagnostic(BackgroundDiagnosticCode.BG_NPC_ID_COLLISION, Severity.ERROR,
                    "Resource ID collision: " + exception.getMessage(), Optional.empty()));
            return failed(diagnostics);
        }
        diagnostics.addAll(new BackgroundCatalogValidator().validate(catalog));
        return diagnostics.stream().anyMatch(BackgroundDiagnostic::error) ? failed(diagnostics)
                : new BackgroundCompileResult(Optional.of(catalog), diagnostics);
    }

    private List<ParsedBackgroundScript> parse(List<BackgroundScriptSource> sources,
            List<BackgroundDiagnostic> diagnostics) {
        List<ParsedBackgroundScript> parsed = new ArrayList<>();
        for (BackgroundScriptSource source : sources) {
            var result = parser.parse(ParseRequest.ofV1(new SourceId(source.sourceId()), source.text()));
            result.diagnostics().forEach(value -> diagnostics.add(new BackgroundDiagnostic(
                    BackgroundDiagnosticCode.BG_NPC_SCRIPT_INVALID,
                    value.severity() == DiagnosticSeverity.ERROR ? Severity.ERROR : Severity.WARNING,
                    value.code() + ": " + value.message(), value.span().map(span -> new BackgroundSourceRef(
                            source.sourceId(), span.start().line(), span.start().column())))));
            Optional<BackgroundResourceContext> context = BackgroundResourceContext.resolve(source.sourceId());
            if (result.successful() && result.syntaxTree().isPresent() && context.isPresent()) {
                parsed.add(new ParsedBackgroundScript(context.orElseThrow(), result.syntaxTree().orElseThrow()));
            } else if (context.isEmpty()) {
                diagnostics.add(new BackgroundDiagnostic(BackgroundDiagnosticCode.BG_NPC_SCRIPT_INVALID,
                        Severity.ERROR, "Не удалось вывести ResourceId из пути.", Optional.empty()));
            }
        }
        return List.copyOf(parsed);
    }

    private Map<String, BackgroundNpcId> symbols(List<ParsedBackgroundScript> parsed,
            List<BackgroundDiagnostic> diagnostics) {
        Map<String, BackgroundNpcId> symbols = new LinkedHashMap<>();
        parsed.stream().filter(value -> value.unit().classDeclaration().baseType().name().text()
                .equals("BackgroundNpcResource")).forEach(value -> {
                    String symbol = value.unit().classDeclaration().name();
                    BackgroundNpcId id = new BackgroundNpcId(value.context().resourceId());
                    if (symbols.putIfAbsent(symbol, id) != null) diagnostics.add(new BackgroundDiagnostic(
                            BackgroundDiagnosticCode.BG_NPC_ID_COLLISION, Severity.ERROR,
                            "Повторяющийся symbol: " + symbol, Optional.empty()));
                });
        return Map.copyOf(symbols);
    }

    private BackgroundDiagnostic error(String message, ParsedBackgroundScript script) {
        var span = script.unit().classDeclaration().span();
        return new BackgroundDiagnostic(BackgroundDiagnosticCode.BG_NPC_SCRIPT_INVALID, Severity.ERROR,
                message == null ? "Ошибка semantic binding." : message,
                Optional.of(new BackgroundSourceRef(script.context().sourceId(), span.start().line(),
                        span.start().column())));
    }
    private BackgroundCompileResult failed(List<BackgroundDiagnostic> diagnostics) {
        return new BackgroundCompileResult(Optional.empty(), diagnostics);
    }
}
