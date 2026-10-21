package io.github.kxyr0.echoiteminspection.model;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/** Semantic output consumed by Content Doctor without reparsing the source. */
public record ItemInspectionDescriptor(
        ItemInspectionId id, String itemId, ItemInspectionView view,
        SourceSpan declarationSpan, SourceSpan itemSpan, ItemInspectionText text
) {
    public ItemInspectionDescriptor(ItemInspectionId id, String itemId, ItemInspectionView view,
            SourceSpan declarationSpan, SourceSpan itemSpan) {
        this(id, itemId, view, declarationSpan, itemSpan, ItemInspectionText.defaults());
    }

    public ItemInspectionDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(declarationSpan, "declarationSpan");
        Objects.requireNonNull(itemSpan, "itemSpan");
        Objects.requireNonNull(text, "text");
    }
}
