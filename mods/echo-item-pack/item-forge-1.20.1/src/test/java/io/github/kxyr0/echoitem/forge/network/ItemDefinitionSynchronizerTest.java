package io.github.kxyr0.echoitem.forge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemDefinitionSynchronizerTest {
    @Test
    void clientSnapshotUpdatesMirrorAndCatalog() {
        EchoItemId id = EchoItemId.parse("jdh:client_sync_blade");
        var definition = WeaponDefinitionCatalog.unavailable(id, new ItemDefinitionRevision(2L));
        ItemCatalogSnapshot snapshot = ItemCatalogSnapshot.empty().withWeapons(List.of(definition));
        WeaponDefinitionCatalog catalog = new WeaponDefinitionCatalog();

        ItemDefinitionSynchronizer synchronizer = new ItemDefinitionSynchronizer(catalog);
        synchronizer.applyClientSnapshot(snapshot);

        assertEquals(snapshot, synchronizer.clientMirror());
        assertEquals(definition, catalog.current(id));
    }
}
