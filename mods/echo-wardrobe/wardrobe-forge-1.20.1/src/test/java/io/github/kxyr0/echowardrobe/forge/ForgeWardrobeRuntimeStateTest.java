package io.github.kxyr0.echowardrobe.forge;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.definition.WearableItemBinding;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.profile.CoveragePolicy;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.OutfitVariantRevision;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableDefinitionRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.catalog.OutfitVariantCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeProfileCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WearableCatalog;
import io.github.kxyr0.echowardrobe.core.equipment.EquipmentItemSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ForgeWardrobeRuntimeStateTest {
    private static final WardrobeProfileId PROFILE = WardrobeProfileId.parse("jdh:john");
    private static final WearableId TOP = WearableId.parse("jdh:test_top");

    @Test
    void publishedCatalogRecomputesAppearanceFromKnownEquipment() {
        ForgeWardrobeRuntimeState state = new ForgeWardrobeRuntimeState();
        UUID playerUuid = UUID.randomUUID();
        state.publishCatalog(catalog(1L, "jdh:skins/first"));
        state.observe(playerUuid,
                new EquipmentItemSnapshot(Map.of(WardrobeEquipmentSlot.CHEST, TOP.toString())));
        long firstAppearanceRevision = state.appearanceRevision(playerUuid);

        state.publishCatalog(catalog(2L, "jdh:skins/second"));

        Assertions.assertEquals("jdh:skins/second", state.outfit(playerUuid).orElseThrow().worldSkin());
        Assertions.assertTrue(state.appearanceRevision(playerUuid) > firstAppearanceRevision);
    }

    private WardrobeCatalog catalog(long revision, String skin) {
        WearableDefinition wearable = new WearableDefinition(TOP, PROFILE, WardrobeSlot.TOP, "test",
                new WearableItemBinding(TOP.toString(), WardrobeEquipmentSlot.CHEST), Optional.empty(),
                Optional.empty(), WearableDefinitionStatus.ACTIVE, new WearableDefinitionRevision(revision),
                WearableSourceFingerprint.builtin("top-" + revision));
        OutfitKey key = new OutfitKey(PROFILE, WearableSelection.EMPTY, WearableSelection.of(TOP),
                WearableSelection.EMPTY, WearableSelection.EMPTY);
        OutfitVariantDefinition outfit = new OutfitVariantDefinition(
                OutfitVariantId.parse("jdh:test/outfit_" + revision), PROFILE, key, skin, skin,
                OutfitVariantStatus.ACTIVE, new OutfitVariantRevision(revision),
                WearableSourceFingerprint.builtin("outfit-" + revision));
        WardrobeProfileDefinition profile = new WardrobeProfileDefinition(PROFILE,
                ForgeWardrobeRuntimeState.DEFAULT_BASE_SKIN, Set.of(WardrobeSlot.TOP), CoveragePolicy.REPORT,
                new WardrobeCatalogRevision(revision), WearableSourceFingerprint.builtin("profile-" + revision));
        return new WardrobeCatalog(WardrobeProfileCatalog.of(List.of(profile)),
                WearableCatalog.of(List.of(wearable), revision).value().orElseThrow(),
                OutfitVariantCatalog.of(List.of(outfit)).value().orElseThrow(),
                new WardrobeCatalogRevision(revision));
    }
}
