package io.github.kxyr0.echocontentdoctor.graph;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import java.util.List;
import java.util.Map;

public record ContentGraph(
        ContentCatalogSnapshot snapshot,
        Map<Key, List<ContentDescriptor>> resources,
        Map<Key, List<ContentReference>> reverseReferences
) {
    public ContentGraph {
        resources = Map.copyOf(resources);
        reverseReferences = Map.copyOf(reverseReferences);
    }

    public List<ContentDescriptor> resolve(ContentResourceKind kind, ContentResourceId id) {
        return resources.getOrDefault(new Key(kind, id), List.of());
    }

    public record Key(ContentResourceKind kind, ContentResourceId id) { }
}
