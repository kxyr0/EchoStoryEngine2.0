package io.github.kxyr0.echowardrobe.core.catalog;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.definition.WearableItemBinding;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WearableDefinitionRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WearableCatalogTest {
    @Test
    void emptyCatalogIsValidW1State() {
        WearableCatalog catalog = WearableCatalog.empty();

        Assertions.assertEquals(0, catalog.count());
        Assertions.assertEquals(0L, catalog.revision());
    }

    @Test
    void duplicateWearableIdsAreRejected() {
        WardrobeResult<WearableCatalog> result = WearableCatalog.of(List.of(definition(), definition()), 1L);

        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_WEARABLE_DUPLICATE));
    }

    @Test
    void activeWearablesAreIndexedByItemAndProfileSlot() {
        WearableCatalog catalog = WearableCatalog.of(List.of(definition()), 1L).value().orElseThrow();

        Assertions.assertTrue(catalog.activeByItemId("jdh:origin_john_robe").isPresent());
        Assertions.assertEquals(1, catalog.byProfileAndSlot(WardrobeProfileId.parse("jdh:john"),
                WardrobeSlot.TOP).size());
    }

    private WearableDefinition definition() {
        return new WearableDefinition(WearableId.parse("jdh:origin_john_robe"),
                WardrobeProfileId.parse("jdh:john"), WardrobeSlot.TOP, "origin",
                new WearableItemBinding("jdh:origin_john_robe", WardrobeEquipmentSlot.CHEST),
                Optional.empty(), Optional.empty(), WearableDefinitionStatus.ACTIVE,
                new WearableDefinitionRevision(0L), WearableSourceFingerprint.builtin("test"));
    }
}
