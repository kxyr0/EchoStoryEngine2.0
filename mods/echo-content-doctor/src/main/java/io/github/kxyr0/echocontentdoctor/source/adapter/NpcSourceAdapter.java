package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnosticSeverity;
import io.github.kxyr0.echonpc.echoscript.EchoScriptNpcCompiler;
import java.nio.file.Path;
import java.util.List;

public final class NpcSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_npc/npcs/") && value.endsWith(".npc.es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var result = new EchoScriptNpcCompiler().compile(sourceId, sourceText);
        List<ContentSourceProblem> problems = result.diagnostics().stream()
                .filter(value -> value.severity() != NpcDiagnosticSeverity.INFO)
                .map(value -> new ContentSourceProblem(
                        value.severity() == NpcDiagnosticSeverity.ERROR ? ContentSeverity.ERROR : ContentSeverity.WARNING,
                        value.code(), SourceIdentities.inferred(sourceId), sourceId, sourceText,
                        value.line().orElse(1), value.column().orElse(1), value.message())).toList();
        return result.definition().map(value -> new AdaptedSource(List.of(ContentDescriptor.simple(
                new ContentResourceId(value.id().value()), ContentResourceKind.NPC,
                SourceSpans.file(sourceId, sourceText), List.of())), problems))
                .orElseGet(() -> new AdaptedSource(List.of(), problems));
    }
}
