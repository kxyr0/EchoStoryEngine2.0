package io.github.kxyr0.echoitem.forge.network;

import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest immutable client mirror snapshot after server sync.
 */
public final class ItemDefinitionSynchronizer {
    private final AtomicReference<ItemCatalogSnapshot> clientMirror = new AtomicReference<>(ItemCatalogSnapshot.empty());
    private final WeaponDefinitionCatalog catalog;

    public ItemDefinitionSynchronizer(WeaponDefinitionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public void applyClientSnapshot(ItemCatalogSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        clientMirror.set(snapshot);
        catalog.publish(snapshot);
    }

    public ItemCatalogSnapshot clientMirror() {
        return clientMirror.get();
    }
}
