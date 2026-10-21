package io.github.kxyr0.echocontentdoctor.graph;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ContentGraphBuilder {
    public ContentGraph build(ContentCatalogSnapshot snapshot) {
        Map<ContentGraph.Key, List<ContentDescriptor>> resources = new HashMap<>();
        Map<ContentGraph.Key, List<ContentReference>> reverse = new HashMap<>();
        for (ContentDescriptor descriptor : snapshot.resources()) {
            ContentGraph.Key key = new ContentGraph.Key(descriptor.kind(), descriptor.id());
            resources.computeIfAbsent(key, ignored -> new ArrayList<>()).add(descriptor);
            for (ContentReference reference : descriptor.references()) {
                ContentGraph.Key target = new ContentGraph.Key(reference.kind().targetKind(), reference.target());
                reverse.computeIfAbsent(target, ignored -> new ArrayList<>()).add(reference);
            }
        }
        resources.replaceAll((ignored, values) -> List.copyOf(values));
        reverse.replaceAll((ignored, values) -> List.copyOf(values));
        return new ContentGraph(snapshot, resources, reverse);
    }
}
