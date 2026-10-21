package io.github.kxyr0.echowardrobe.forge.api;

import io.github.kxyr0.echowardrobe.forge.api.appearance.WardrobeAppearanceView;
import io.github.kxyr0.echowardrobe.forge.api.catalog.WearableCatalogView;
import io.github.kxyr0.echowardrobe.forge.api.diagnostics.WardrobeDiagnosticsView;
import io.github.kxyr0.echowardrobe.forge.api.outfit.OutfitVariantCatalogView;
import io.github.kxyr0.echowardrobe.forge.api.player.PlayerWardrobeView;
import io.github.kxyr0.echowardrobe.forge.api.profile.WardrobeProfileCatalogView;
import io.github.kxyr0.echowardrobe.forge.api.view.WearableModelView;

/**
 * Public service surface exposed by the Forge runtime host.
 */
public interface EchoWardrobeService extends WearableCatalogView, OutfitVariantCatalogView,
        WardrobeProfileCatalogView, PlayerWardrobeView, WearableModelView, WardrobeAppearanceView,
        WardrobeDiagnosticsView {
}
