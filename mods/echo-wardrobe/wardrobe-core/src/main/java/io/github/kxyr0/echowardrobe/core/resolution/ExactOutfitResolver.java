package io.github.kxyr0.echowardrobe.core.resolution;

import io.github.kxyr0.echowardrobe.api.contribution.WardrobeAppearanceContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfitFallback;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.runtime.EquippedWearableSnapshot;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import io.github.kxyr0.echowardrobe.core.equipment.EquipmentItemSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Exact OutfitKey resolver. No closest, wildcard, or registration-order fallback is used.
 */
public final class ExactOutfitResolver {
    private final WearableItemResolver itemResolver = new WearableItemResolver();

    public WardrobeResult<ResolvedOutfit> resolve(
            WardrobeProfileId profileId,
            EquipmentItemSnapshot equipment,
            WardrobeCatalog catalog,
            String defaultBaseSkin,
            long appearanceRevision
    ) {
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        EquippedWearableSnapshot snapshot = itemResolver.resolve(profileId, equipment, catalog.wearables(),
                catalog.revision().value(), diagnostics);
        OutfitKey key = key(snapshot);
        String baseSkin = baseSkin(profileId, catalog).orElse(defaultBaseSkin);
        Optional<ResolvedOutfit> exact = catalog.outfits().exact(key).map(variant -> new ResolvedOutfit(profileId,
                key, Optional.of(variant.id()), variant.worldSkin(), variant.faceSkin(), models(snapshot, catalog),
                ResolvedOutfitFallback.NONE, catalog.revision(), appearanceRevision));
        if (exact.isPresent()) {
            return WardrobeResult.valid(exact.orElseThrow(), diagnostics);
        }
        diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_OUTFIT_VARIANT_NOT_FOUND,
                "Exact outfit variant was not found for " + key.serialize(), profileId.toString()));
        ResolvedOutfit fallback = new ResolvedOutfit(profileId, key, Optional.empty(), baseSkin, baseSkin, List.of(),
                ResolvedOutfitFallback.PROFILE_DEFAULT, catalog.revision(), appearanceRevision);
        return WardrobeResult.valid(fallback, diagnostics);
    }

    public WardrobeAppearanceContribution contribution(ResolvedOutfit outfit) {
        return new WardrobeAppearanceContribution(outfit.profileId(), outfit.outfitKey(), outfit.outfitVariantId(),
                outfit.worldSkin(), outfit.faceSkin(), outfit.models(), outfit.catalogRevision(),
                outfit.appearanceRevision());
    }

    private OutfitKey key(EquippedWearableSnapshot snapshot) {
        return new OutfitKey(snapshot.profileId(), selection(snapshot, WardrobeSlot.FACE),
                selection(snapshot, WardrobeSlot.TOP), selection(snapshot, WardrobeSlot.BOTTOM),
                selection(snapshot, WardrobeSlot.FEET));
    }

    private WearableSelection selection(EquippedWearableSnapshot snapshot, WardrobeSlot slot) {
        return snapshot.selections().getOrDefault(slot, WearableSelection.EMPTY);
    }

    private Optional<String> baseSkin(WardrobeProfileId id, WardrobeCatalog catalog) {
        return catalog.profiles().find(id).map(WardrobeProfileDefinition::baseSkin);
    }

    private List<WearableModelContribution> models(EquippedWearableSnapshot snapshot, WardrobeCatalog catalog) {
        List<WearableModelContribution> models = new ArrayList<>();
        for (WearableSelection selection : snapshot.selections().values()) {
            selection.wearableId()
                    .flatMap(id -> catalog.wearables().find(id))
                    .flatMap(definition -> definition.model())
                    .ifPresent(models::add);
        }
        return List.copyOf(models);
    }
}
