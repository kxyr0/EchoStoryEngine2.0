package io.github.kxyr0.echowardrobe.core.coverage;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.catalog.OutfitVariantCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WearableCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds explicit authoring coverage only on demand.
 */
public final class WardrobeCoverageAnalyzer {
    public CoverageReport analyze(
            WardrobeProfileId profileId,
            WearableCatalog wearables,
            OutfitVariantCatalog outfits
    ) {
        List<WearableSelection> faces = selections(profileId, WardrobeSlot.FACE, wearables);
        List<WearableSelection> tops = selections(profileId, WardrobeSlot.TOP, wearables);
        List<WearableSelection> bottoms = selections(profileId, WardrobeSlot.BOTTOM, wearables);
        List<WearableSelection> feet = selections(profileId, WardrobeSlot.FEET, wearables);
        List<OutfitKey> missing = new ArrayList<>();
        int mapped = 0;
        for (WearableSelection face : faces) {
            for (WearableSelection top : tops) {
                for (WearableSelection bottom : bottoms) {
                    for (WearableSelection foot : feet) {
                        OutfitKey key = new OutfitKey(profileId, face, top, bottom, foot);
                        if (outfits.exact(key).isPresent()) {
                            mapped++;
                        } else {
                            missing.add(key);
                        }
                    }
                }
            }
        }
        int total = faces.size() * tops.size() * bottoms.size() * feet.size();
        return new CoverageReport(profileId, total, mapped, missing, 0, unused(profileId, outfits));
    }

    private List<WearableSelection> selections(
            WardrobeProfileId profileId,
            WardrobeSlot slot,
            WearableCatalog catalog
    ) {
        List<WearableSelection> result = new ArrayList<>();
        result.add(WearableSelection.EMPTY);
        catalog.byProfileAndSlot(profileId, slot).stream()
                .map(definition -> WearableSelection.of(definition.id()))
                .forEach(result::add);
        return List.copyOf(result);
    }

    private int unused(WardrobeProfileId profileId, OutfitVariantCatalog outfits) {
        return (int) outfits.byProfile(profileId).stream()
                .filter(variant -> outfits.exact(variant.key()).isEmpty())
                .count();
    }
}
