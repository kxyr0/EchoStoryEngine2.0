package io.github.kxyr0.echodebug.forge.client.catalog;

import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import java.util.List;

public final class ClientAreaCatalog {
    private List<TriggerAreaDefinitionView> areas = List.of();

    public void apply(List<TriggerAreaDefinitionView> areas) {
        this.areas = List.copyOf(areas);
    }

    public List<TriggerAreaDefinitionView> areas() {
        return areas;
    }

    public void clear() {
        areas = List.of();
    }
}
