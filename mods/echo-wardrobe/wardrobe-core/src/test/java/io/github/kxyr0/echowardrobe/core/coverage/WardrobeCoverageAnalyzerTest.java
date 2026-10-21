package io.github.kxyr0.echowardrobe.core.coverage;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.definition.WearableItemBinding;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.OutfitVariantRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableDefinitionRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.catalog.OutfitVariantCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WearableCatalog;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WardrobeCoverageAnalyzerTest {
    private static final WardrobeProfileId PROFILE = WardrobeProfileId.parse("jdh:john");

    @Test
    void reportsExplicitMissingCombinationsWithoutExpandingAtRuntime() {
        WearableCatalog wearables = WearableCatalog.of(List.of(
                wearable("jdh:origin_john_glasses", WardrobeSlot.FACE, WardrobeEquipmentSlot.HEAD),
                wearable("jdh:origin_john_robe", WardrobeSlot.TOP, WardrobeEquipmentSlot.CHEST)), 1L)
                .value().orElseThrow();
        OutfitVariantCatalog outfits = OutfitVariantCatalog.of(List.of(
                variant("jdh:john/base", OutfitKey.empty(PROFILE)),
                variant("jdh:john/glasses_robe", new OutfitKey(PROFILE,
                        WearableSelection.of(WearableId.parse("jdh:origin_john_glasses")),
                        WearableSelection.of(WearableId.parse("jdh:origin_john_robe")),
                        WearableSelection.EMPTY, WearableSelection.EMPTY)))).value().orElseThrow();

        CoverageReport report = new WardrobeCoverageAnalyzer().analyze(PROFILE, wearables, outfits);

        Assertions.assertEquals(4, report.totalExpected());
        Assertions.assertEquals(2, report.mapped());
        Assertions.assertEquals(2, report.missing().size());
    }

    private WearableDefinition wearable(String id, WardrobeSlot slot, WardrobeEquipmentSlot equipment) {
        return new WearableDefinition(WearableId.parse(id), PROFILE, slot, "origin",
                new WearableItemBinding(id, equipment), Optional.empty(), Optional.empty(),
                WearableDefinitionStatus.ACTIVE, new WearableDefinitionRevision(1L),
                WearableSourceFingerprint.builtin(id));
    }

    private OutfitVariantDefinition variant(String id, OutfitKey key) {
        return new OutfitVariantDefinition(OutfitVariantId.parse(id), PROFILE, key,
                "jdh:echo_wardrobe/skins/origin/example", "jdh:echo_wardrobe/skins/origin/example",
                OutfitVariantStatus.ACTIVE, new OutfitVariantRevision(1L), WearableSourceFingerprint.builtin(id));
    }
}
