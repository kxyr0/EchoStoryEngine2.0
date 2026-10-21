package io.github.kxyr0.echoitem.core.catalog;

import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import java.util.Collection;
import java.util.Objects;

/**
 * Publishes complete immutable catalog snapshots atomically.
 */
public final class ItemCatalogPublisher {
    private final WeaponDefinitionCatalog catalog;

    public ItemCatalogPublisher(WeaponDefinitionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public ItemCatalogSnapshot publish(Collection<WeaponDefinition> definitions) {
        ItemCatalogSnapshot next = catalog.snapshot().withWeapons(definitions);
        catalog.publish(next);
        return next;
    }
}
