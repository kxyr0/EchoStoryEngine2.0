package io.github.kxyr0.echoiteminspection.catalog;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionId;
import java.util.List;
import java.util.Optional;

/** Immutable catalog publication; definitions retained by an open session cannot change. */
public record InspectionCatalogSnapshot(
        ItemInspectionDefinitionRevision revision, List<ItemInspectionDefinition> definitions
) {
    public InspectionCatalogSnapshot {
        definitions = List.copyOf(definitions);
    }

    public static InspectionCatalogSnapshot empty() {
        return new InspectionCatalogSnapshot(new ItemInspectionDefinitionRevision(0), List.of());
    }

    public Optional<ItemInspectionDefinition> resolve(String itemId) {
        return definitions.stream().filter(value -> value.itemId().equals(itemId)).findFirst();
    }

    public Optional<ItemInspectionDefinition> byId(ItemInspectionId id) {
        return definitions.stream().filter(value -> value.id().equals(id)).findFirst();
    }
}
