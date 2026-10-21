package io.github.kxyr0.echoitem.core.catalog;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic catalog reference used by runtime items.
 */
public final class WeaponDefinitionCatalog {
    private final AtomicReference<ItemCatalogSnapshot> current = new AtomicReference<>(ItemCatalogSnapshot.empty());

    public ItemCatalogSnapshot snapshot() {
        return current.get();
    }

    public void publish(ItemCatalogSnapshot snapshot) {
        current.set(snapshot);
    }

    public WeaponDefinition current(EchoItemId id) {
        return snapshot().weapon(id).orElseGet(() -> unavailable(id, snapshot().revision()));
    }

    public static WeaponDefinition unavailable(EchoItemId id, ItemDefinitionRevision revision) {
        return new WeaponDefinition(id, new ItemTextDefinition("Unavailable Echo Item", "Definition is missing.",
                List.of("Safe placeholder retained by registry manifest.")),
                ItemVisualDefinition.handheld("echo_item:item/missing", 0L),
                new WeaponCombatDefinition(1.0D, 1.0D, 0.0D),
                new WeaponDurabilityDefinition(1, 0, Optional.empty(), false),
                WeaponRarity.COMMON, false, false, Optional.empty(), true, OptionalInt.empty(),
                revision, new ItemSourceFingerprint("0".repeat(64)), false);
    }
}
