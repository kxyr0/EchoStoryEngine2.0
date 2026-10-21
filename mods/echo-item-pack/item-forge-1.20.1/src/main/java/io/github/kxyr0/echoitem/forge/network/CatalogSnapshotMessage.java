package io.github.kxyr0.echoitem.forge.network;

import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

record CatalogSnapshotMessage(ItemCatalogSnapshot snapshot) {
    CatalogSnapshotMessage {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    void encode(FriendlyByteBuf buffer) {
        WeaponDefinitionNetworkCodec.writeSnapshot(buffer, snapshot);
    }

    static CatalogSnapshotMessage decode(FriendlyByteBuf buffer) {
        return new CatalogSnapshotMessage(WeaponDefinitionNetworkCodec.readSnapshot(buffer));
    }
}
