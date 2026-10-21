package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContentCatalogAssembler {
    /** Real registry keys are authoritative; keep source declarations only for registered items. */
    public ContentCatalogSnapshot withItems(ContentCatalogSnapshot source, ItemSnapshotProvider items) {
        List<ContentDescriptor> descriptors = new ArrayList<>(source.resources().stream()
                .filter(value -> value.kind() != ContentResourceKind.ITEM || items.itemIds().contains(value.id().value()))
                .toList());
        var declared = descriptors.stream().filter(value -> value.kind() == ContentResourceKind.ITEM)
                .map(ContentDescriptor::id).collect(java.util.stream.Collectors.toSet());
        items.descriptors().stream().filter(value -> !declared.contains(value.id())).forEach(descriptors::add);
        descriptors.sort(Comparator.comparing(value -> value.id().value()));
        long revision = (31 * source.revision() + items.revision()) & Long.MAX_VALUE;
        return new ContentCatalogSnapshot(revision, source.files() + items.files(), descriptors);
    }

    public ContentCatalogSnapshot assemble(List<ContentSnapshotProvider> providers) {
        long revision = providers.stream().mapToLong(ContentSnapshotProvider::revision).max().orElse(0);
        int files = providers.stream().mapToInt(ContentSnapshotProvider::files).sum();
        List<ContentDescriptor> descriptors = providers.stream().flatMap(value -> value.descriptors().stream())
                .sorted(Comparator.comparing(value -> value.id().value())).toList();
        return new ContentCatalogSnapshot(revision, files, descriptors);
    }
}
