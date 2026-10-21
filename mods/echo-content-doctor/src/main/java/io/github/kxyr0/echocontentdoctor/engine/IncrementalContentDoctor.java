package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraphBuilder;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IncrementalContentDoctor {
    private final ContentDoctor doctor = new ContentDoctor();

    public ContentDoctorResult validate(ContentDoctorInput input, ContentResourceKind kind, ContentResourceId changed) {
        ContentGraph graph = new ContentGraphBuilder().build(input.catalog());
        Set<ContentGraph.Key> affected = affected(graph, new ContentGraph.Key(kind, changed));
        ContentDoctorResult full = doctor.validate(input);
        List<ContentDiagnostic> diagnostics = full.diagnostics().stream()
                .filter(value -> affected.stream().anyMatch(key -> key.id().equals(value.resource()))).toList();
        return new ContentDoctorResult(full.revision(), full.files(), full.resources(), full.references(), diagnostics);
    }

    private Set<ContentGraph.Key> affected(ContentGraph graph, ContentGraph.Key changed) {
        Set<ContentGraph.Key> visited = new HashSet<>();
        ArrayDeque<ContentGraph.Key> queue = new ArrayDeque<>();
        queue.add(changed);
        while (!queue.isEmpty()) {
            ContentGraph.Key key = queue.removeFirst();
            if (!visited.add(key)) continue;
            graph.snapshot().resources().stream().filter(resource -> resource.references().stream()
                    .anyMatch(reference -> reference.target().equals(key.id())
                            && reference.kind().targetKind() == key.kind()))
                    .map(resource -> new ContentGraph.Key(resource.kind(), resource.id())).forEach(queue::addLast);
        }
        return visited;
    }
}
