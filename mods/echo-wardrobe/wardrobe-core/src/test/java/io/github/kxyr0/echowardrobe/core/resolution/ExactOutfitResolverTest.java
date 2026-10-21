package io.github.kxyr0.echowardrobe.core.resolution;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearableModelTransform;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.definition.WearableItemBinding;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfitFallback;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ExactOutfitResolverTest {
    private static final WardrobeProfileId PROFILE = WardrobeProfileId.parse("jdh:john");
    private static final String BASE_SKIN = "echo_content_player:player/base";

    @Test
    void resolvesOnlyExactOutfitKeyAndCarriesWearableModels() {
        WardrobeCatalog catalog = catalog(List.of(
                wearable("jdh:origin_john_glasses", WardrobeSlot.FACE, WardrobeEquipmentSlot.HEAD,
                        Optional.of(model())),
                wearable("jdh:origin_john_robe", WardrobeSlot.TOP, WardrobeEquipmentSlot.CHEST, Optional.empty()),
                wearable("jdh:origin_john_pants", WardrobeSlot.BOTTOM, WardrobeEquipmentSlot.LEGS, Optional.empty()),
                wearable("jdh:origin_john_boots", WardrobeSlot.FEET, WardrobeEquipmentSlot.FEET, Optional.empty())),
                List.of(fullOriginVariant("jdh:john/full_dressed", "jdh:origin_john_pants")));

        WardrobeResult<ResolvedOutfit> result = new ExactOutfitResolver().resolve(PROFILE, originEquipment(),
                catalog, "fallback:base", 11L);

        Assertions.assertTrue(result.successful());
        ResolvedOutfit outfit = result.value().orElseThrow();
        Assertions.assertEquals(Optional.of(OutfitVariantId.parse("jdh:john/full_dressed")),
                outfit.outfitVariantId());
        Assertions.assertEquals("jdh:echo_wardrobe/skins/origin/jdh_full_dressed", outfit.worldSkin());
        Assertions.assertEquals(ResolvedOutfitFallback.NONE, outfit.fallback());
        Assertions.assertEquals(1, outfit.models().size());
    }

    @Test
    void similarVariantDoesNotCountAsClosestMatch() {
        WardrobeCatalog catalog = catalog(List.of(
                wearable("jdh:origin_john_glasses", WardrobeSlot.FACE, WardrobeEquipmentSlot.HEAD, Optional.empty()),
                wearable("jdh:origin_john_robe", WardrobeSlot.TOP, WardrobeEquipmentSlot.CHEST, Optional.empty()),
                wearable("jdh:origin_john_pants", WardrobeSlot.BOTTOM, WardrobeEquipmentSlot.LEGS, Optional.empty()),
                wearable("jdh:classic_john_pants", WardrobeSlot.BOTTOM, WardrobeEquipmentSlot.LEGS, Optional.empty()),
                wearable("jdh:origin_john_boots", WardrobeSlot.FEET, WardrobeEquipmentSlot.FEET, Optional.empty())),
                List.of(fullOriginVariant("jdh:john/full_dressed", "jdh:origin_john_pants")));
        EquipmentItemSnapshot equipment = equipment("jdh:classic_john_pants");

        WardrobeResult<ResolvedOutfit> result = new ExactOutfitResolver().resolve(PROFILE, equipment,
                catalog, "fallback:base", 12L);

        ResolvedOutfit outfit = result.value().orElseThrow();
        Assertions.assertEquals(Optional.empty(), outfit.outfitVariantId());
        Assertions.assertEquals(BASE_SKIN, outfit.worldSkin());
        Assertions.assertEquals(ResolvedOutfitFallback.PROFILE_DEFAULT, outfit.fallback());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_OUTFIT_VARIANT_NOT_FOUND));
    }

    @Test
    void outfitCatalogUsesIndexedExactLookup() {
        OutfitVariantCatalog outfits = OutfitVariantCatalog.of(List.of(
                fullOriginVariant("jdh:john/full_dressed", "jdh:origin_john_pants"))).value().orElseThrow();

        Assertions.assertTrue(outfits.indexedLookup());
        Assertions.assertTrue(outfits.exact(fullKey("jdh:origin_john_pants")).isPresent());
    }

    private WardrobeCatalog catalog(List<WearableDefinition> wearables, List<OutfitVariantDefinition> outfits) {
        return new WardrobeCatalog(WardrobeProfileCatalog.of(List.of(profile())),
                WearableCatalog.of(wearables, 7L).value().orElseThrow(),
                OutfitVariantCatalog.of(outfits).value().orElseThrow(), new WardrobeCatalogRevision(7L));
    }

    private WardrobeProfileDefinition profile() {
        return new WardrobeProfileDefinition(PROFILE, BASE_SKIN, java.util.Set.of(WardrobeSlot.FACE,
                WardrobeSlot.TOP, WardrobeSlot.BOTTOM, WardrobeSlot.FEET), CoveragePolicy.REPORT,
                new WardrobeCatalogRevision(7L), WearableSourceFingerprint.builtin("profile"));
    }

    private WearableDefinition wearable(String id, WardrobeSlot slot, WardrobeEquipmentSlot equipment,
            Optional<WearableModelContribution> model) {
        return new WearableDefinition(WearableId.parse(id), PROFILE, slot, "origin",
                new WearableItemBinding(id, equipment), model, Optional.empty(), WearableDefinitionStatus.ACTIVE,
                new WearableDefinitionRevision(1L), WearableSourceFingerprint.builtin(id));
    }

    private WearableModelContribution model() {
        return new WearableModelContribution("jdh:item/origin_john_glasses", WearableModelTransform.identity());
    }

    private OutfitVariantDefinition fullOriginVariant(String id, String bottom) {
        return new OutfitVariantDefinition(OutfitVariantId.parse(id), PROFILE, fullKey(bottom),
                "jdh:echo_wardrobe/skins/origin/jdh_full_dressed",
                "jdh:echo_wardrobe/skins/origin/jdh_full_dressed", OutfitVariantStatus.ACTIVE,
                new OutfitVariantRevision(1L), WearableSourceFingerprint.builtin(id));
    }

    private OutfitKey fullKey(String bottom) {
        return new OutfitKey(PROFILE, WearableSelection.of(WearableId.parse("jdh:origin_john_glasses")),
                WearableSelection.of(WearableId.parse("jdh:origin_john_robe")),
                WearableSelection.of(WearableId.parse(bottom)),
                WearableSelection.of(WearableId.parse("jdh:origin_john_boots")));
    }

    private EquipmentItemSnapshot originEquipment() {
        return equipment("jdh:origin_john_pants");
    }

    private EquipmentItemSnapshot equipment(String bottom) {
        Map<WardrobeEquipmentSlot, String> items = new EnumMap<>(WardrobeEquipmentSlot.class);
        items.put(WardrobeEquipmentSlot.HEAD, "jdh:origin_john_glasses");
        items.put(WardrobeEquipmentSlot.CHEST, "jdh:origin_john_robe");
        items.put(WardrobeEquipmentSlot.LEGS, bottom);
        items.put(WardrobeEquipmentSlot.FEET, "jdh:origin_john_boots");
        return new EquipmentItemSnapshot(items);
    }
}
