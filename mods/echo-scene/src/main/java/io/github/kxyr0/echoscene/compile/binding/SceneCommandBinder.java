package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.descriptor.SceneReference;
import io.github.kxyr0.echoscene.api.descriptor.SceneReferenceKind;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.compile.SceneArgumentKind;
import io.github.kxyr0.echoscene.compile.SceneCommandRegistry;
import io.github.kxyr0.echoscene.compile.SceneCommandRule;
import io.github.kxyr0.echoscene.compile.SceneSymbol;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.SceneSymbolKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SceneCommandBinder {
    private static final Set<String> SERVICES = Set.of("Time", "Camera", "Dialogue", "Scene", "Animation", "Effect", "TeleportMagic");
    private final SceneSymbolCatalog symbols;
    private final SceneCommandRegistry commands;
    private final InvocationReader reader = new InvocationReader();

    SceneCommandBinder(SceneSymbolCatalog symbols, SceneCommandRegistry commands) {
        this.symbols = symbols;
        this.commands = commands;
    }

    Optional<BoundCommand> bind(ExpressionSyntax expression, SceneStepId stepId, List<SceneDiagnostic> diagnostics) {
        if (new io.github.kxyr0.echoaudio.script.AudioCommandBinder().accepts(expression)) {
            return new io.github.kxyr0.echoscene.compile.audio.SceneAudioBinder(symbols)
                    .bind(expression, stepId, diagnostics).map(value -> new BoundCommand(value.command(), value.references()));
        }
        Optional<BoundCall> parsed = reader.read(expression);
        if (parsed.isEmpty()) {
            diagnostics.add(error("SCENE_COMMAND_FAILED", "Expected a Scene command invocation.", expression.span()));
            return Optional.empty();
        }
        BoundCall call = parsed.orElseThrow();
        Optional<SceneSymbol> receiver = receiver(call, diagnostics);
        if (receiver.isEmpty()) {
            return Optional.empty();
        }
        Optional<SceneCommandRule> rule = commands.find(receiver.orElseThrow(), call.method());
        if (rule.isEmpty() || call.arguments().size() != rule.orElseThrow().arguments().size()) {
            diagnostics.add(error("SCENE_COMMAND_FAILED", "Unsupported Scene command: "
                    + call.receiver() + "." + call.method(), call.span()));
            return Optional.empty();
        }
        return build(call, receiver.orElseThrow(), rule.orElseThrow(), stepId, diagnostics);
    }

    private Optional<BoundCommand> build(BoundCall call, SceneSymbol receiver, SceneCommandRule rule,
            SceneStepId stepId, List<SceneDiagnostic> diagnostics) {
        List<String> arguments = new ArrayList<>();
        List<SceneReference> references = new ArrayList<>();
        for (int index = 0; index < call.arguments().size(); index++) {
            BoundCall.BoundValue value = call.arguments().get(index);
            SceneArgumentKind kind = rule.arguments().get(index);
            Optional<String> resolved = resolveArgument(value, kind, references, diagnostics);
            if (resolved.isEmpty()) {
                return Optional.empty();
            }
            arguments.add(resolved.orElseThrow());
        }
        actorReference(receiver, call.span()).ifPresent(references::add);
        var capabilities = new ArrayList<>(rule.capabilities().apply(receiver));
        if (receiver.name().equals("TeleportMagic") && !arguments.isEmpty()) {
            symbols.symbols().stream().filter(s -> s.resourceId().equals(arguments.get(0)))
                    .filter(s -> s.kind() == SceneSymbolKind.NPC || s.kind() == SceneSymbolKind.PLAYER)
                    .findFirst().ifPresent(s -> capabilities.add(new io.github.kxyr0.echoscene.api.definition.SceneCapability(
                            s.kind() == SceneSymbolKind.PLAYER
                                    ? io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind.PLAYER
                                    : io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind.NPC, s.resourceId())));
        }
        SceneCommand command = new SceneCommand(stepId, receiver.resourceId(), call.method(), arguments,
                rule.blockingMode(), capabilities, call.span());
        return Optional.of(new BoundCommand(command, references));
    }

    private Optional<SceneSymbol> receiver(BoundCall call, List<SceneDiagnostic> diagnostics) {
        if (SERVICES.contains(call.receiver())) {
            return Optional.of(new SceneSymbol(call.receiver(), call.receiver(), SceneSymbolKind.SERVICE));
        }
        List<SceneSymbol> matches = symbols.resolve(call.receiver());
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        String code = matches.isEmpty() ? "SCENE_SYMBOL_NOT_FOUND" : "SCENE_SYMBOL_AMBIGUOUS";
        diagnostics.add(error(code, "Scene symbol '" + call.receiver() + "' resolved to " + matches.size()
                + " candidates.", call.span()));
        return Optional.empty();
    }

    private Optional<String> resolveArgument(BoundCall.BoundValue value, SceneArgumentKind kind,
            List<SceneReference> references, List<SceneDiagnostic> diagnostics) {
        if (kind == SceneArgumentKind.VALUE) {
            return Optional.of(value.value());
        }
        SceneSymbolKind expected = symbolKind(kind);
        List<SceneSymbol> matches = symbols.symbols().stream()
                .filter(symbol -> (value.literal() ? symbol.resourceId() : symbol.name()).equals(value.value()))
                .filter(symbol -> symbol.kind() == expected
                        || kind == SceneArgumentKind.ACTOR && symbol.kind() == SceneSymbolKind.PLAYER).toList();
        if (matches.size() != 1) {
            String code = matches.isEmpty() ? "SCENE_SYMBOL_NOT_FOUND" : "SCENE_SYMBOL_AMBIGUOUS";
            String suggestions = String.join(", ", symbols.suggest(value.value(), expected));
            diagnostics.add(error(code, kind + " '" + value.value() + "' resolved to " + matches.size()
                    + " candidates. Available: " + suggestions, value.span()));
            return Optional.empty();
        }
        SceneSymbol resolved = matches.get(0);
        references.add(new SceneReference(kind.referenceKind().orElseThrow(), resolved.resourceId(), value.span()));
        return Optional.of(resolved.resourceId());
    }

    private Optional<SceneReference> actorReference(SceneSymbol symbol,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) {
        return symbol.kind() == SceneSymbolKind.NPC || symbol.kind() == SceneSymbolKind.PLAYER
                ? Optional.of(new SceneReference(SceneReferenceKind.ACTOR, symbol.resourceId(), span))
                : Optional.empty();
    }

    private SceneSymbolKind symbolKind(SceneArgumentKind kind) {
        return switch (kind) {
            case ACTOR -> SceneSymbolKind.NPC;
            case LOCATION -> SceneSymbolKind.LOCATION;
            case CAMERA -> SceneSymbolKind.CAMERA;
            case DIALOGUE -> SceneSymbolKind.DIALOGUE;
            case ANIMATION -> SceneSymbolKind.ANIMATION;
            case SCENE -> SceneSymbolKind.SCENE;
            case EFFECT -> SceneSymbolKind.EFFECT;
            case OBSERVATION -> SceneSymbolKind.OBSERVATION;
            case ITEM -> SceneSymbolKind.ITEM;
            case VALUE -> throw new IllegalArgumentException("Value has no symbol kind.");
        };
    }

    private SceneDiagnostic error(String code, String message,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) {
        return new SceneDiagnostic(SceneDiagnostic.Severity.ERROR, code, message, span);
    }

    record BoundCommand(SceneCommand command, List<SceneReference> references) {
        BoundCommand {
            references = List.copyOf(references);
        }
    }
}
