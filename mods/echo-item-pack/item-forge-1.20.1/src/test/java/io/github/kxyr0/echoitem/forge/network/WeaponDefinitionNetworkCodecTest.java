package io.github.kxyr0.echoitem.forge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.equipment.ItemEquipmentSlot;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class WeaponDefinitionNetworkCodecTest {
    @Test
    void snapshotRoundTripsWeaponDefinitions() {
        EchoItemId id = EchoItemId.parse("jdh:test_sync_blade");
        WeaponDefinition definition = new WeaponDefinition(id,
                new ItemTextDefinition("Sync Blade", "Server authoritative.", List.of("hot", "reload")),
                new ItemVisualDefinition("jdh:item/sync_blade", Optional.of("jdh:item/sync_blade_custom"),
                        true, 9L),
                new WeaponCombatDefinition(7.0D, 3.0D, 1.0D),
                new WeaponDurabilityDefinition(181, 12, Optional.of("minecraft:iron_ingot"), false),
                WeaponRarity.RARE, true, true, Optional.of(ItemEquipmentSlot.CHEST), false, OptionalInt.of(4),
                new ItemDefinitionRevision(3L), new ItemSourceFingerprint("a".repeat(64)), true);
        ItemCatalogSnapshot snapshot = new ItemCatalogSnapshot(new ItemDefinitionRevision(5L), Map.of(id,
                definition));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        WeaponDefinitionNetworkCodec.writeSnapshot(buffer, snapshot);
        ItemCatalogSnapshot decoded = WeaponDefinitionNetworkCodec.readSnapshot(buffer);

        assertEquals(snapshot.revision(), decoded.revision());
        assertEquals(definition, decoded.weapon(id).orElseThrow());
    }
}
