package io.github.kxyr0.echocontentdoctor.source.adapter.events;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentNode;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import java.nio.file.Path;
import java.util.List;

public final class ObservationSourceAdapter implements SourceAdapter {
    private final io.github.kxyr0.echocontentdoctor.source.adapter.audio.AudioActionSourceAdapter audio;
    public ObservationSourceAdapter(io.github.kxyr0.echoscene.compile.SceneSymbolCatalog symbols) {
        audio = new io.github.kxyr0.echocontentdoctor.source.adapter.audio.AudioActionSourceAdapter(symbols);
    }
    public boolean accepts(Path path) {
        return path.toString().replace('\\', '/').contains("/echo_observation/observations/") && path.toString().endsWith(".es");
    }
    public AdaptedSource adapt(String sourceId, String text) {
        var inspected = audio.adapt(sourceId, text);
        var result = new ObservationResourceCompiler().compile(
                SourceIdentities.inferred(sourceId).namespace() + ":" + sourceId, text);
        var problems = new java.util.ArrayList<>(inspected.problems());
        problems.addAll(result.diagnostics().stream().map(value -> new ContentSourceProblem(
                value.error() ? ContentSeverity.ERROR : ContentSeverity.WARNING, value.code().name(),
                SourceIdentities.inferred(sourceId), sourceId, text, 1, 1, value.message())).toList());
        var references = inspected.descriptors().stream().flatMap(value -> value.references().stream()).toList();
        var descriptors = result.resource().stream().map(resource -> ContentDescriptor.simple(
                new ContentResourceId(resource.id().toString()), ContentResourceKind.OBSERVATION,
                SourceSpans.file(sourceId, text), references).withNodes(resource.observations().stream()
                        .map(point -> new ContentNode(point.id().local().value(), true, true, List.of(),
                                SourceSpans.find(sourceId, text, point.id().local().value()))).toList())).toList();
        return new AdaptedSource(descriptors.isEmpty() ? inspected.descriptors() : descriptors, problems);
    }
}
