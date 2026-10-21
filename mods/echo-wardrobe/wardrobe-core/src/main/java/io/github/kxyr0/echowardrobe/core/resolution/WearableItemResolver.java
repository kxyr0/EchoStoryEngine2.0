package io.github.kxyr0.echowardrobe.core.resolution;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.runtime.EquippedWearableSnapshot;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.catalog.WearableCatalog;
import io.github.kxyr0.echowardrobe.core.equipment.EquipmentItemSnapshot;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves vanilla equipment item IDs into typed wardrobe selections.
 */
public final class WearableItemResolver {
    public EquippedWearableSnapshot resolve(
            WardrobeProfileId profileId,
            EquipmentItemSnapshot equipment,
            WearableCatalog wearables,
            long catalogRevision,
            List<WardrobeDiagnostic> diagnostics
    ) {
        Objects.requireNonNull(profileId, "profileId");
        Map<WardrobeSlot, String> items = new EnumMap<>(WardrobeSlot.class);
        Map<WardrobeSlot, WearableSelection> selections = emptySelections();
        for (WardrobeSlot slot : WardrobeSlot.values()) {
            equipment.itemId(slot.equipmentSlot()).ifPresent(itemId -> {
                items.put(slot, itemId);
                wearables.activeByItemId(itemId).ifPresent(definition ->
                        select(profileId, slot, definition, selections, diagnostics));
            });
        }
        return new EquippedWearableSnapshot(profileId, items, selections, catalogRevision);
    }

    private void select(WardrobeProfileId profileId, WardrobeSlot slot, WearableDefinition definition,
            Map<WardrobeSlot, WearableSelection> selections, List<WardrobeDiagnostic> diagnostics) {
        if (definition.slot() != slot) {
            diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_EQUIPMENT_SLOT_MISMATCH,
                    "Equipped item belongs to " + definition.slot() + " but was observed in " + slot,
                    definition.id().toString()));
            return;
        }
        if (!definition.profileId().equals(profileId)) {
            diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_PROFILE_MISMATCH,
                    "Equipped wearable belongs to another wardrobe profile.", definition.id().toString()));
            return;
        }
        selections.put(slot, WearableSelection.of(definition.id()));
    }

    private Map<WardrobeSlot, WearableSelection> emptySelections() {
        Map<WardrobeSlot, WearableSelection> selections = new EnumMap<>(WardrobeSlot.class);
        for (WardrobeSlot slot : WardrobeSlot.values()) {
            selections.put(slot, WearableSelection.EMPTY);
        }
        return selections;
    }
}
