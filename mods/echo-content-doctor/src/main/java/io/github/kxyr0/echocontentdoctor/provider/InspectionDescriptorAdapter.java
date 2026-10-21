package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDescriptor;
import java.util.List;

/** Consumes the compiler's typed descriptors without parsing source a second time. */
public record InspectionDescriptorAdapter(long revision, int files,
        List<ItemInspectionDescriptor> inspections) implements ContentSnapshotProvider {
    public InspectionDescriptorAdapter { inspections = List.copyOf(inspections); }

    @Override
    public List<ContentDescriptor> descriptors() {
        return inspections.stream().map(InspectionDescriptorAdapter::adapt).toList();
    }

    public static ContentDescriptor adapt(ItemInspectionDescriptor descriptor) {
        var reference = new ContentReference(ContentReferenceKind.ITEM,
                new ContentResourceId(descriptor.itemId()), descriptor.itemSpan());
        return ContentDescriptor.simple(new ContentResourceId(descriptor.id().value()),
                ContentResourceKind.ITEM_INSPECTION, descriptor.declarationSpan(), List.of(reference));
    }
}
