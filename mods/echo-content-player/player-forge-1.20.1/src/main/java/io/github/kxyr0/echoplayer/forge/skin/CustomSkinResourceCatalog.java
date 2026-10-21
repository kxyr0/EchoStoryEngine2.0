package io.github.kxyr0.echoplayer.forge.skin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves authored skin IDs to Minecraft texture resource locations.
 */
public final class CustomSkinResourceCatalog {
    public Optional<ResourceLocation> resolve(PlayerAppearanceSnapshot appearance) {
        if (appearance.skinMode() != PlayerSkinMode.RESOURCE) {
            return Optional.empty();
        }
        return appearance.skinId().map(id -> new ResourceLocation(id.namespace(), id.resourcePath()));
    }
}
