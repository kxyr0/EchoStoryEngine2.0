package io.github.kxyr0.echocontentdoctor.source.adapter.audio;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.api.AudioFailure;
import io.github.kxyr0.echoaudio.script.AudioScriptInspection;
import io.github.kxyr0.echocontentdoctor.model.*;
import io.github.kxyr0.echocontentdoctor.source.*;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.SceneSymbolKind;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.nio.file.Path;
import java.util.*;

/** Audio descriptors for generic completion blocks; full Observation semantics stay with its own compiler. */
public final class AudioActionSourceAdapter implements SourceAdapter {
    private final SceneSymbolCatalog symbols;
    public AudioActionSourceAdapter(SceneSymbolCatalog symbols) { this.symbols = symbols; }
    @Override public boolean accepts(Path path) {
        String text = path.toString().replace('\\', '/');
        return text.contains("/echo_observation/observations/") && text.endsWith(".es");
    }
    @Override public AdaptedSource adapt(String sourceId, String sourceText) {
        var id = SourceIdentities.inferred(sourceId);
        List<ContentSourceProblem> problems = new ArrayList<>();
        List<ContentReference> references = new ArrayList<>();
        var parse = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        if (!parse.successful()) {
            parse.diagnostics().forEach(d -> problems.add(new ContentSourceProblem(ContentSeverity.ERROR, d.code(), id,
                    sourceId, sourceText, d.span().map(s -> s.start().line()).orElse(1),
                    d.span().map(s -> s.start().column()).orElse(1), d.message())));
            return new AdaptedSource(List.of(), problems);
        }
        new AudioScriptInspection().inspect(parse.syntaxTree().orElseThrow(), (cue, span) -> {
            cue.requireCompletionSafe();
            if (!cue.resource().isEmpty()) references.add(new ContentReference(ContentReferenceKind.AUDIO,
                    new ContentResourceId(cue.resource()), span));
            location(cue, span, references);
        }, (failure, span) -> problems.add(new ContentSourceProblem(ContentSeverity.ERROR, failure.code(), id,
                sourceId, sourceText, span.start().line(), span.start().column(), failure.getMessage())));
        return new AdaptedSource(List.of(ContentDescriptor.simple(id, ContentResourceKind.OBSERVATION,
                SourceSpans.file(sourceId, sourceText), references)), problems);
    }
    private void location(AudioCue cue, SourceSpan span, List<ContentReference> references) {
        if (cue.location().isEmpty()) return;
        var matches = symbols.resolve(cue.location()).stream().filter(s -> s.kind() == SceneSymbolKind.LOCATION).toList();
        if (matches.isEmpty()) matches = symbols.resolveResource(cue.location(), SceneSymbolKind.LOCATION);
        if (matches.size() != 1) throw new AudioFailure("AUDIO_LOCATION_NOT_FOUND", cue.location());
        references.add(new ContentReference(ContentReferenceKind.LOCATION, new ContentResourceId(matches.get(0).resourceId()), span));
    }
}
