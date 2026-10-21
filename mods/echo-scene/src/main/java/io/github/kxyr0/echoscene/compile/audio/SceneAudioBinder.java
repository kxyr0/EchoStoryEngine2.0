package io.github.kxyr0.echoscene.compile.audio;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echoscript.api.syntax.expression.*;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.descriptor.*;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import io.github.kxyr0.echoscene.compile.*;
import java.util.*;

public final class SceneAudioBinder {
    private final SceneSymbolCatalog symbols;
    public SceneAudioBinder(SceneSymbolCatalog symbols) { this.symbols = symbols; }
    public Optional<Result> bind(ExpressionSyntax expression, SceneStepId step,
            List<SceneDiagnostic> diagnostics) {
        try {
            AudioCue cue = new AudioCommandBinder().bind((InvocationExpressionSyntax) expression);
            List<SceneReference> references = new ArrayList<>();
            if (!cue.operation().equals("Stop")) references.add(new SceneReference(SceneReferenceKind.AUDIO, cue.resource(), expression.span()));
            if (!cue.location().isEmpty()) {
                List<SceneSymbol> matches = symbols.resolve(cue.location()).stream()
                        .filter(s -> s.kind() == SceneSymbolKind.LOCATION).toList();
                if (matches.isEmpty()) matches = symbols.resolveResource(cue.location(), SceneSymbolKind.LOCATION);
                if (matches.size() != 1) throw new AudioFailure("AUDIO_LOCATION_NOT_FOUND", cue.location());
                String location = matches.get(0).resourceId();
                references.add(new SceneReference(SceneReferenceKind.LOCATION, location, expression.span()));
                cue = new AudioCue(cue.operation(), cue.resource(), cue.name(), location, cue.options(), cue.source());
            }
            var command = new SceneCommand(step, "Sound", cue.operation(), cue.arguments(),
                    cue.waitForEnd() ? BlockingMode.DURABLE_BLOCKING : BlockingMode.FIRE_AND_CONTINUE,
                    List.of(), expression.span());
            return Optional.of(new Result(command, references));
        } catch (AudioFailure failure) {
            diagnostics.add(new SceneDiagnostic(SceneDiagnostic.Severity.ERROR, failure.code(), failure.getMessage(), expression.span()));
            return Optional.empty();
        }
    }
    public record Result(SceneCommand command, List<SceneReference> references) { }
}
