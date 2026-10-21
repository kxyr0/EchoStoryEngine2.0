package io.github.kxyr0.echowardrobe.api;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelTransform;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfitFallback;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WardrobeContractsTest {
    @Test
    void wearableIdRequiresNamespaceAndPath() {
        WearableId id = WearableId.parse("jdh:origin_john_glasses");

        Assertions.assertEquals("jdh", id.namespace());
        Assertions.assertEquals("origin_john_glasses", id.resourcePath());
        Assertions.assertThrows(IllegalArgumentException.class, () -> WearableId.parse("RoundGlasses"));
    }

    @Test
    void wardrobeSlotsMapToVanillaEquipmentNames() {
        Assertions.assertEquals(WardrobeEquipmentSlot.HEAD, WardrobeSlot.FACE.equipmentSlot());
        Assertions.assertEquals(WardrobeEquipmentSlot.CHEST, WardrobeSlot.TOP.equipmentSlot());
        Assertions.assertEquals(WardrobeEquipmentSlot.LEGS, WardrobeSlot.BOTTOM.equipmentSlot());
        Assertions.assertEquals(WardrobeEquipmentSlot.FEET, WardrobeSlot.FEET.equipmentSlot());
    }

    @Test
    void outfitKeySerializesInFixedSlotOrderWithTypedEmptySelection() {
        WardrobeProfileId profile = WardrobeProfileId.parse("jdh:john");
        OutfitKey key = new OutfitKey(profile, WearableSelection.of(WearableId.parse("jdh:origin_john_glasses")),
                WearableSelection.of(WearableId.parse("jdh:origin_john_robe")),
                WearableSelection.of(WearableId.parse("jdh:origin_john_pants")), WearableSelection.EMPTY);

        Assertions.assertEquals("jdh:john|FACE=jdh:origin_john_glasses|TOP=jdh:origin_john_robe"
                + "|BOTTOM=jdh:origin_john_pants|FEET=-", key.serialize());
        Assertions.assertEquals(WearableSelection.EMPTY, key.selection(WardrobeSlot.FEET));
        Assertions.assertEquals("-", WearableSelection.EMPTY.serialize());
    }

    @Test
    void resolvedOutfitCarriesExactVariantAndFallbackBoundary() {
        WardrobeProfileId profile = WardrobeProfileId.parse("jdh:john");
        OutfitKey key = OutfitKey.empty(profile);
        ResolvedOutfit outfit = new ResolvedOutfit(profile, key, Optional.empty(),
                "echo_content_player:player/base", "echo_content_player:player/base", List.of(),
                ResolvedOutfitFallback.PROFILE_DEFAULT, new WardrobeCatalogRevision(3L), 9L);

        Assertions.assertEquals(ResolvedOutfitFallback.PROFILE_DEFAULT, outfit.fallback());
        Assertions.assertEquals("echo_content_player:player/base", outfit.worldSkin());
        Assertions.assertEquals(9L, outfit.appearanceRevision());
    }

    @Test
    void wearableModelTransformRejectsNonPositiveScale() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new WearableModelTransform(0D, 0D, 0D, 0D, 0D, 0D, 0D, 1D, 1D));
    }
}
