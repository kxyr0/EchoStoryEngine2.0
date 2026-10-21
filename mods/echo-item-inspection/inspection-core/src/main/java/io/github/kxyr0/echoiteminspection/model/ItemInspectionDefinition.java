package io.github.kxyr0.echoiteminspection.model;

import java.util.Objects;

/** Immutable definition; a session keeps this value when later catalogs are published. */
public record ItemInspectionDefinition(
        ItemInspectionId id, String itemId, ItemInspectionDefinitionRevision revision,
        ItemInspectionView view, ItemInspectionDescriptor descriptor, ItemInspectionText text
) {
    public ItemInspectionDefinition(ItemInspectionId id, String itemId, ItemInspectionDefinitionRevision revision,
            ItemInspectionView view, ItemInspectionDescriptor descriptor) {
        this(id, itemId, revision, view, descriptor, descriptor.text());
    }

    public ItemInspectionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(text, "text");
        if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid item ID: " + itemId);
        }
    }
}
