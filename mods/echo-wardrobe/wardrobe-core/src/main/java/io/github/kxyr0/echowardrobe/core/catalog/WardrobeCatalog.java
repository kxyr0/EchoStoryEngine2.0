package io.github.kxyr0.echowardrobe.core.catalog;

import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import java.util.Objects;

/**
 * Accepted immutable wardrobe catalog snapshot.
 */
public record WardrobeCatalog(
        WardrobeProfileCatalog profiles,
        WearableCatalog wearables,
        OutfitVariantCatalog outfits,
        WardrobeCatalogRevision revision
) {
    public WardrobeCatalog {
        profiles = Objects.requireNonNull(profiles, "profiles");
        wearables = Objects.requireNonNull(wearables, "wearables");
        outfits = Objects.requireNonNull(outfits, "outfits");
        revision = Objects.requireNonNull(revision, "revision");
    }

    public static WardrobeCatalog empty() {
        return new WardrobeCatalog(WardrobeProfileCatalog.empty(), WearableCatalog.empty(),
                OutfitVariantCatalog.empty(), new WardrobeCatalogRevision(0L));
    }
}
