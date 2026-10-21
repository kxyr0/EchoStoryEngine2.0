package io.github.kxyr0.echocontentdoctor.graph;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import java.util.List;

public record ContentCatalogSnapshot(long revision, int files, List<ContentDescriptor> resources) {
    public ContentCatalogSnapshot {
        if (revision < 0 || files < 0) throw new IllegalArgumentException("Catalog counters must not be negative.");
        resources = List.copyOf(resources);
    }
}
